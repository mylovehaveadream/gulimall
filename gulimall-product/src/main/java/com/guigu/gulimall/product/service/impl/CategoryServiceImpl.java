package com.guigu.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guigu.common.utils.PageUtils;
import com.guigu.common.utils.Query;

import com.guigu.gulimall.product.dao.CategoryDao;
import com.guigu.gulimall.product.entity.CategoryEntity;
import com.guigu.gulimall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

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