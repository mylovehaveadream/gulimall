package com.guigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.guigu.gulimall.product.service.CategoryBrandRelationService;
import com.guigu.gulimall.product.vo.Catelog2Vo;
import org.checkerframework.checker.units.qual.A;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guigu.common.utils.PageUtils;
import com.guigu.common.utils.Query;

import com.guigu.gulimall.product.dao.CategoryDao;
import com.guigu.gulimall.product.entity.CategoryEntity;
import com.guigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查出所有分类
        //baseMapper就是CategoryDao,因为ServiceImpl里面有泛型的注入
        //没有查询条件就是查询所有，所以写null
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2.组装成父子的树形结构

        //2.1.找到所有的一级分类
        List<CategoryEntity> levelMenus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu) -> {
            //找到当前菜单的所有子菜单，给他设置进去
            menu.setChildren(getChildrens(menu, entities));
            return menu;    //映射好的菜单返回
        }).sorted((menu1, menu2) -> {
            //给当前映射好的菜单进行排序
            //前一个菜单和后一个菜单进行对比，返回对比的结果
            return (menu1.getSort() == null ? 0 : menu1.getSort()) -
                    (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());//把这些排完序的数据收集成一个集合

        return levelMenus;
    }


    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 这是一个提示以后要做的功能，做完就删除了，备忘录
        //TODO 检查当前删除的菜单，是否被别的地方引用，如果引用了不能删除(以后要做的，现在不做)

        //逻辑删除
        /**
         * 用到逻辑删除的表，都要设计相关的状态标志位
         * Mybatis-Plus
         * 1.配置全局的逻辑删除规则 （可省）
         * 2.配置逻辑删除的组件Bean（可省）
         * 3.给bean加上逻辑删除注解@TableLogic
         *
         */

        //这个用的不多
        baseMapper.deleteBatchIds(asList);  //批量的删除方法，物理删除
    }


    /**
     * 找到catelogId的完整路径
     * [父/子/孙]
     * [2,25,225]
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();

        List<Long> parentPath = findParentPath(catelogId, paths);
        //进行一个转换，逆序出来
        Collections.reverse(parentPath);

        return parentPath.toArray(new Long[parentPath.size()]);
    }

    //级联更新所有关联的数据
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        //更新完自己
        this.updateById(category);
        //更新关联表里面的
        categoryBrandRelationService.updateCategory
                (category.getCatId(), category.getName());

        //同时修改缓存中的数据
        //redis.del("catalogJSON") 删除缓存中的数据；等待下一次主动查询进行更新
    }

    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> categoryEntities =
                baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));

        return categoryEntities;
    }


    /**
     * TODO 产生堆外内存溢出：OutOfDirectMemoryError
     * 1.SpringBoot2.0以后默认使用lettuce作为操作redis的客户端。它使用netty进行网络通信
     * 2.lettuce的bug导致netty堆外内存溢出 -Xmx300m; netty如果没有指定堆外内存，默认使用-Xmx300m指定的值作为堆外内存
     * 可以通过-Dio.netty.maxDirectMemory进行设置
     * <p>
     * 解决方案：不能使用-Dio.netty.maxDirectMemory只去调大堆外内存。调大了内存只是延缓出现这个异常，并没有解决问题
     * 1.升级lettuce客户端
     * 2.切换使用jedis,排除lettuce
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        //给缓存中放JSON字符串，拿出的JSON字符串，还要逆转为能用的对象类型，【序列化与反序列化】

        /**
         * 1.空结果缓存、解决缓存穿透
         * 2.设置过期时间（加随机值）：解决缓存雪崩
         * 3.加锁：解决缓存击穿
         */

        //1.加入缓存逻辑，缓存中存的数据是JSON字符串。
        //存为JSON的好处是JSON跨语言，跨平台兼容，以后给缓存中保存复杂对象，都存为JSON数据
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            System.out.println("缓存不命中。。。查询数据库");
            //2.缓存中没有，查询数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedisLock();

            return catalogJsonFromDb;
        }

        System.out.println("缓存命中。。。直接返回");
        //转为我们指定的对象
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,
                new TypeReference<Map<String, List<Catelog2Vo>>>() {
                });

        return result;
    }


    /**
     * 缓存里面的数据如何和数据库保持一致
     * 缓存数据一致性
     * 1.双写模式
     * 2.失效模式
     */
    //使用Redisson来做分布式锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

        //1.锁的名字，名字一样锁就一样，锁的粒度，越细越快，反之就越慢
        //锁的粒度:具体缓存的是某个数据，11-号商品：product-11-lock product-12-lock
        RLock lock = redisson.getLock("catalogJson-lock");
        lock.lock();    //加锁了，下面的代码是一个阻塞等待

        Map<String, List<Catelog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock();
        }

        return dataFromDb;
    }


    //分布式锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {

        //1.占分布式锁，去redis占坑
        String uuid = UUID.randomUUID().toString();

        //返回true/false
        //序机不想做了，把锁时间放长一点
        Boolean lock = redisTemplate.opsForValue()
                .setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);//就是redis的SETNX

        if (lock) {
            System.out.println("获取分布式锁成功");
            //加锁成功,占到坑位了,执行业务

            //2.设置过期时间,必须和加锁是同步的，原子的
            //redisTemplate.expire("lock",30,TimeUnit.SECONDS);   //30秒以后自动删
            Map<String, List<Catelog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                //获取值对比+对比成功删除=原子操作     使用lua脚本解锁
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                //删除成功返回1，不成功返回0,Integer返回值类型
                //原子删除锁
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                        Arrays.asList("lock"), uuid);
            }

            //执行成功需要解锁,别人还要把坑给占到，需要把锁给删除了
            //redisTemplate.delete("lock");   //删除锁，别人能占成功了


//            String lockValue = redisTemplate.opsForValue().get("lock");
//            if(uuid.equals(lockValue)){
//                //删除我自己的锁
//                redisTemplate.delete("lock");   //删除锁，别人能占成功了
//            }

            return dataFromDb;
        } else {
            //加锁失败，重试, synchronized ()这个是一个自旋的方式，一直在重试重试
            //重试太频繁，可以休眠100ms后重试
            System.out.println("获取分布式锁失败。。。等待重试");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDbWithRedisLock(); //自旋的方式
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {
            //缓存不为空直接返回
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON,
                    new TypeReference<Map<String, List<Catelog2Vo>>>() {
                    });

            return result;
        }

        System.out.println("查询了数据库。。。");
        /**
         * 1.将数据库的多次查询变为一次
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);//不传条件查询所有

        //1.查出所有1级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        //2.封装数据
        //收集映射成map,k是一级分类的CatId，v是
        Map<String, List<Catelog2Vo>> parent_cid =
                level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    //1.每一个的一级分类，查到这个一级分类的二级分类
                    //v就是当前遍历的一级分类
                    List<CategoryEntity> categoryEntities =
                            getParent_cid(selectList, v.getCatId());

                    //2.封装上面的结果
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        //当前分类的二级分类的集合
                        catelog2Vos = categoryEntities.stream().map(l2 -> {   //遍历二级分类的信息
                            Catelog2Vo catelog2Vo =
                                    new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());

                            //1.找当前二级分类的三级分类封装成vo
                            List<CategoryEntity> level3Catelog =
                                    getParent_cid(selectList, l2.getCatId());

                            if (level3Catelog != null) {
                                List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                                    //2.封装成指定格式
                                    Catelog2Vo.Catelog3Vo catelog3Vo =
                                            new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                                    return catelog3Vo;
                                }).collect(Collectors.toList());

                                catelog2Vo.setCatalog3List(collect);
                            }

                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }

                    return catelog2Vos;
                }));

        //3.查到的数据再放入缓存,将对象转为JSON放在缓存中
        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);  //设置过期时间

        return parent_cid;
    }

    //本地锁
    //从数据库去查询并封装分类数据
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {

        //只要是同一把锁，就能锁住需要这个锁的所有线程
        //1. synchronized (this);springboot所有的组件在容器中都是单例的
        //  这个service只有一个实例对象，所以这个this是单例的，就能锁住了
        // this只代表当前实例的这个对象
        // 本地锁在分布式情况下锁不住我们所有的服务，所以需要分布式锁
        //当前是本地锁,只锁当前进程的
        //TODO 本地锁：synchronized，JUC（Lock），在分布式情况下，想要锁住所有，必须使用分布式锁

        synchronized (this) {
            //得到锁以后，我们应该再去缓存中确定一次，如果没有才需要继续查询
            return getDataFromDb();
        }
    }


    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
//        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));

        List<CategoryEntity> collect =
                selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        return collect;
    }

    //225,25,2
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);//当前分类

        if (byId.getParentCid() != 0) {//找父分类
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }


    //使用递归，找到所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == root.getCatId()
        ).map(categoryEntity -> {   //子菜单下还有子菜单，进行递归查找
            //1.找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            //2.菜单排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) -
                    (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());    //子菜单排好序以后，收集返回

        return children;
    }

}