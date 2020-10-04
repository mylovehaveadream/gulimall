package com.guigu.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.guigu.common.constant.ProductConstant;
import com.guigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.guigu.gulimall.product.dao.AttrGroupDao;
import com.guigu.gulimall.product.dao.CategoryDao;
import com.guigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.guigu.gulimall.product.entity.AttrGroupEntity;
import com.guigu.gulimall.product.entity.CategoryEntity;
import com.guigu.gulimall.product.service.CategoryService;
import com.guigu.gulimall.product.vo.AttrGroupRelationVo;
import com.guigu.gulimall.product.vo.AttrRespVO;
import com.guigu.gulimall.product.vo.AttrVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guigu.common.utils.PageUtils;
import com.guigu.common.utils.Query;

import com.guigu.gulimall.product.dao.AttrDao;
import com.guigu.gulimall.product.entity.AttrEntity;
import com.guigu.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {
    @Autowired
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        //复制属性，将页面来到值，最终封装到PO里面
        //前提条件，他们两个属性名之间要是一一对应的
        BeanUtils.copyProperties(attr,attrEntity);
        //保存基本数据
        this.save(attrEntity);
        //保存关联关系
        if(attr.getAttrType() == ProductConstant.AttrEnum.
                ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId()!=null){ //基本属性才保存分组关系
            AttrAttrgroupRelationEntity entity = new AttrAttrgroupRelationEntity();
            //属性分组的id
            entity.setAttrGroupId(attr.getAttrGroupId());
            //属性id
            entity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationDao.insert(entity);
        }

    }

    @Override
    public PageUtils queryBaseAttr(Map<String, Object> params, Long catelogId, String type) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                .eq("attr_type","base".equalsIgnoreCase(type)?
                        ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode():
                        ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());

        if(catelogId !=0){
            queryWrapper.eq("catelog_id",catelogId);
        }

        String key = (String) params.get("key");

        if(!StringUtils.isEmpty(key)){
            queryWrapper.and((wrapper)->{
               wrapper.eq("attr_id",key).or()
                       .like("attr_name",key);
            });
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        PageUtils pageUtils = new PageUtils(page);

        List<AttrEntity> records = page.getRecords();

        List<AttrRespVO> respVOS = records.stream().map((attrEntity) -> {
            AttrRespVO attrRespVO = new AttrRespVO();
            BeanUtils.copyProperties(attrEntity, attrRespVO);

            //设置分组的名字
            if("base".equalsIgnoreCase(type)){
                AttrAttrgroupRelationEntity attrId =
                        attrAttrgroupRelationDao.selectOne(
                                new QueryWrapper<AttrAttrgroupRelationEntity>().
                                        eq("attr_id", attrEntity.getAttrId()));
                if (attrId != null && attrId.getAttrGroupId()!=null) {
                    AttrGroupEntity attrGroupEntity =
                            attrGroupDao.selectById(attrId.getAttrGroupId());
                    attrRespVO.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }

            //设置分类的名字
            CategoryEntity categoryEntity =
                    categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVO.setCatelogName(categoryEntity.getName());
            }

            return attrRespVO;
        }).collect(Collectors.toList());

        pageUtils.setList(respVOS);

        return pageUtils;
    }

    @Override
    public AttrRespVO getAttrInfo(Long attrId) {
        AttrRespVO respVO = new AttrRespVO();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity,respVO);

        if(attrEntity.getAttrType() ==
                ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            //1.设置分组信息
            AttrAttrgroupRelationEntity attrgroupRelationEntity =
                    attrAttrgroupRelationDao.selectOne(
                            new QueryWrapper<AttrAttrgroupRelationEntity>().
                                    eq("attr_id", attrId));
            if(attrgroupRelationEntity != null){
                respVO.setAttrGroupId(attrgroupRelationEntity.getAttrGroupId());
                AttrGroupEntity attrGroupEntity =
                        attrGroupDao.selectById(attrgroupRelationEntity.getAttrGroupId());

                if(attrGroupEntity != null) {
                    respVO.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }


        //2.设置分类信息
        Long catelogId = attrEntity.getCatelogId();

        Long[] catelogPath = categoryService.findCatelogPath(catelogId);

        respVO.setCatelogPath(catelogPath);

        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if(categoryEntity != null) {
            respVO.setCatelogName(categoryEntity.getName());
        }

        return respVO;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr,attrEntity);
        this.updateById(attrEntity);

        if(attrEntity.getAttrType() ==
                ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            //1.修改分组关联
            AttrAttrgroupRelationEntity attrgroupRelationEntity =
                    new AttrAttrgroupRelationEntity();

            attrgroupRelationEntity.setAttrGroupId(attr.getAttrGroupId());
            attrgroupRelationEntity.setAttrId(attr.getAttrId());

            //根据条件来统计数量
            Integer count = attrAttrgroupRelationDao.selectCount(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().
                            eq("attr_id", attr.getAttrId()));
            if(count>0){
                attrAttrgroupRelationDao.update(attrgroupRelationEntity,
                        new UpdateWrapper<AttrAttrgroupRelationEntity>().
                                eq("attr_id",attr.getAttrId()));
            }else {
                //新增分组
                attrAttrgroupRelationDao.insert(attrgroupRelationEntity);
            }
        }

    }

    //根据分组id查找关联的所有基本属性
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> entities
                = attrAttrgroupRelationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().
                        eq("attr_group_id", attrgroupId));

        List<Long> attrIds = entities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        if(attrIds == null || attrIds.size() ==0){
            return null;
        }
        Collection<AttrEntity> attrEntities = this.listByIds(attrIds);
        return (List<AttrEntity>) attrEntities;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {
//        attrAttrgroupRelationDao.delete(new QueryWrapper<>().
//                eq("attr_id").eq("attr_group_id"))
        //批量删除
        List<AttrAttrgroupRelationEntity> collect =
                Arrays.asList(vos).stream().map((item) -> {
            AttrAttrgroupRelationEntity entity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, entity);
            return entity;
        }).collect(Collectors.toList());

        attrAttrgroupRelationDao.deleteBatchRelation(collect);
    }

    //获取当前分组没有关联的所有属性
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        //1.当前分组只能关联自己所属的分类里面的所有属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();

        //2.当前分组只能关联别的分组没有引用的属性
        //2.1.当前分类下的其他分组
        List<AttrGroupEntity> group =
                attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().
                eq("catelog_id", catelogId));
        List<Long> collect = group.stream().map((item) -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());

        //2.2.这些分组关联的属性
        List<AttrAttrgroupRelationEntity> groupId =
                attrAttrgroupRelationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().
                        in("attr_group_id", collect));
        List<Long> attrIds = groupId.stream().map((item) -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        //2.3.从当前分类的所有属性中移除这些属性
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>().
                eq("catelog_id", catelogId).
                eq("attr_type",ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());

        if(attrIds !=null && attrIds.size()>0){
            queryWrapper.notIn("attr_id", attrIds);
        }

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
           queryWrapper.and((wrapper)->{
               wrapper.eq("attr_id",key).or().like("attr_name",key);
           }) ;
        }

        IPage<AttrEntity> page =
                this.page(new Query<AttrEntity>().getPage(params), queryWrapper);

        PageUtils pageUtils = new PageUtils(page);

        return pageUtils;
    }

    /**
     *
     * 在指定的所有属性集合里面，挑出检索属性
     */
    @Override
    public List<Long> selectSearchAttrIds(List<Long> attrIds) {
        //select attr_id from `pms_attr` where attr_id in(?) and search_type = 1
        return baseMapper.selectSearchAttrIds(attrIds);
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

}