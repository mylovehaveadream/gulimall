package com.guigu.gulimall.product.service.impl;

import com.guigu.gulimall.product.service.CategoryBrandRelationService;
import com.guigu.gulimall.product.vo.Catelog2Vo;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
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


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

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
        }).map((menu)->{
            //找到当前菜单的所有子菜单，给他设置进去
            menu.setChildren(getChildrens(menu,entities));
            return menu;    //映射好的菜单返回
        }).sorted((menu1,menu2)->{
            //给当前映射好的菜单进行排序
            //前一个菜单和后一个菜单进行对比，返回对比的结果
            return (menu1.getSort()==null?0:menu1.getSort()) -
                    (menu2.getSort()==null?0:menu2.getSort());
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
     *找到catelogId的完整路径
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
                (category.getCatId(),category.getName());
    }

    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> categoryEntities =
                baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));

        return categoryEntities;
    }

    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        //1.查出所有1级分类
        List<CategoryEntity> level1Categorys = getLevel1Categorys();

        //2.封装数据
        //收集映射成map,k是一级分类的CatId，v是
        Map<String, List<Catelog2Vo>> parent_id =
                level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1.每一个的一级分类，查到这个一级分类的二级分类
            //v就是当前遍历的一级分类
            List<CategoryEntity> categoryEntities =
                    baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_id", v.getCatId()));

            //2.封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                //当前分类的二级分类的集合
                catelog2Vos = categoryEntities.stream().map(l2 -> {   //遍历二级分类的信息
                    Catelog2Vo catelog2Vo =
                            new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());

                    //1.找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog =
                            baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_id", l2.getCatId()));

                    if(level3Catelog != null){
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            //2.封装成指定格式
                            Catelog2Vo.Catelog3Vo catelog3Vo =
                                    new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(),l3.getCatId().toString(),l3.getName());

                            return catelog3Vo;
                        }).collect(Collectors.toList());

                        catelog2Vo.setCatalog3List(collect);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

        return parent_id;
    }

    //225,25,2
    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);//当前分类

        if(byId.getParentCid()!=0){//找父分类
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;
    }


    //使用递归，找到所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){
        List<CategoryEntity> children = all.stream().filter(categoryEntity ->
            categoryEntity.getParentCid() == root.getCatId()
        ).map(categoryEntity -> {   //子菜单下还有子菜单，进行递归查找
            //1.找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity,all));
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            //2.菜单排序
            return (menu1.getSort()==null?0:menu1.getSort()) -
                    (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());    //子菜单排好序以后，收集返回

        return children;
    }

}