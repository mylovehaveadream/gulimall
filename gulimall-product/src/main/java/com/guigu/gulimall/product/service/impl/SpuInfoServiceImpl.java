package com.guigu.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.guigu.common.constant.ProductConstant;
import com.guigu.common.to.SkuHasStockVo;
import com.guigu.common.to.SkuReductionTo;
import com.guigu.common.to.SpuBoundTo;
import com.guigu.common.to.es.SkuEsModel;
import com.guigu.common.utils.R;
import com.guigu.gulimall.product.entity.*;
import com.guigu.gulimall.product.feign.CooponFeignService;
import com.guigu.gulimall.product.feign.SearchFeignService;
import com.guigu.gulimall.product.feign.WareFeignService;
import com.guigu.gulimall.product.service.*;
import com.guigu.gulimall.product.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guigu.common.utils.PageUtils;
import com.guigu.common.utils.Query;

import com.guigu.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {
    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CooponFeignService cooponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * TODO 高级部分完善
     *  @GlobalTransactional
     *
     * @param vo
     */
    //这里适合Seata AT分布式事务，没有很高的并发
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        //1.保存spu基本信息 pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());

        this.saveBaseSpuInfo(spuInfoEntity);

        //2.保存spu的描述图片 pms_spu_info_desc
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();

        descEntity.setSpuId(spuInfoEntity.getId());
        //用逗号接起来
        descEntity.setDecript(String.join(",",decript));
        //保存
        spuInfoDescService.saveSpuInfoDesc(descEntity);

        //3.保存spu的图片集 pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveImages(spuInfoEntity.getId(),images);

        //4.保存spu的规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setAttrId(attr.getAttrId());
            AttrEntity byId = attrService.getById(attr.getAttrId());
            valueEntity.setAttrName(byId.getAttrName());
            valueEntity.setAttrValue(attr.getAttrValues());
            valueEntity.setQuickShow(attr.getShowDesc());
            valueEntity.setSpuId(spuInfoEntity.getId());

            return valueEntity;
        }).collect(Collectors.toList());

        productAttrValueService.saveProductAttr(collect);


        //5.保存当前spu对应的所有sku信息
        List<Skus> skus = vo.getSkus();
        if(skus !=null && skus.size()>0){
            skus.forEach(item->{
                String defaultImg = "";
                for (Images image : item.getImages()) {
                    if(image.getDefaultImg() == 1){
                        defaultImg = image.getImgUrl();
                    }
                }
                /**
                 * private String skuName;
                 * private BigDecimal price;
                 * private String skuTitle;
                 * private String skuSubtitle;
                 */
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item,skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                //5.1.sku基本信息 pms_sku_info
                skuInfoService.saveSkuInfo(skuInfoEntity);

                Long skuId = skuInfoEntity.getSkuId();

                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                   skuImagesEntity.setSkuId(skuId);
                   skuImagesEntity.setImgUrl(img.getImgUrl());
                   skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity->{
                    //返回true就是需要，false就是剔除
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());
                //5.2.sku的图片信息 pms_sku_images
                //TODO 没有图片路径的，无需保存
                skuImagesService.saveBatch(imagesEntities);

                //5.3.sku的销售属性信息 pms_sku_sale_attr_value
                List<Attr> attr = item.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(a -> {
                    SkuSaleAttrValueEntity attrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(a, attrValueEntity);
                    attrValueEntity.setSkuId(skuId);

                    return attrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

                //5.4.保存sku的优惠、满减等信息 gulimall_sms->sms_sku_ladder/
                //sms_sku_full_reduction/sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item,skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if(skuReductionTo.getFullCount() > 0 ||
                        skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1){
                    R r = cooponFeignService.saveSkuReduction(skuReductionTo);

                    if(r.getCode() != 0){
                        log.error("远程保存spu优惠信息失败");
                    }
                }

            });
        }

        //6.保存spu的积分信息 gulimall_sms->sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds,spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        R r = cooponFeignService.saveSpuBounds(spuBoundTo);

        if(r.getCode() != 0){
            log.error("远程保存spu积分信息失败");
        }

    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and(w ->{
                w.eq("id",key).or().like("spu_name",key);
            });
        }
        //status=1 and (id =1 or spu_name like xxx)所以写成上面的样子
        String status = (String) params.get("status");
        if(!StringUtils.isEmpty(status)){
            wrapper.eq("publish_status",status);
        }

        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) &&
                !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id",brandId);
        }

        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) &&
                !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id",catelogId);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     *  商品上架
     */
    @Override
    public void up(Long spuId) {
        //1.查出当前spuId对应的所有sku信息，品牌的名字
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);

        //获取所有skuId的集合
        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

        //TODO 4.查询当前sku的所有可以被用来检索的规格属性，实际上这个list里面的sku属性都是一样的，只查一次
        //获取当前spu对应的所有attr信息
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrlistforspu(spuId);
        //收集所有属性的id
        List<Long> attrIds = baseAttrs.stream().map(attr -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        //过滤出都是检索属性的attr，返回attr_id
        List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);

        //从baseAttrs上面的集合里面，过滤出只属于searchAttrIds集合的内容
        Set<Long> idSet = new HashSet<>(searchAttrIds);

        List<SkuEsModel.Attrs> attrsList = baseAttrs.stream().filter(item -> {
            //如果返回true就用
            return idSet.contains(item.getAttrId());
        }).map(item -> {
            SkuEsModel.Attrs attrs1 = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs1);
            return attrs1;
        }).collect(Collectors.toList());


        //TODO 1.发送远程调用，库存系统查询是否有库存
        Map<Long, Boolean> stockMap = null;
        try {   //放在try防止网络波动，产生失败，抛出异常，不走下面了
            R r = wareFeignService.getSkuHasStock(skuIdList);

            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>() {};
            //Map里面key就是skuId,值就是到底有没有东西
            stockMap = r.getData(typeReference).stream()
                            .collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
        }catch (Exception e){
            log.error("库存服务查询异常：原因{}",e);
        }



        //2.封装每个sku的信息
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skus.stream().map(sku -> {
            //组装需要的数据
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku,esModel);

            //不一样的的属性skuPrice,skuImg
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());

            //hasStock,hotScore,brandName,brandImg,catalogName
            //设置库存信息
            if(finalStockMap == null){
                //即使有问题，也给他有数据
                esModel.setHasStock(true);
            }else {
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }


            //TODO 2.热度评分：0
            esModel.setHotScore(0L);

            //TODO 3.查询品牌和分类的名字信息
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());

            CategoryEntity category = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(category.getName());


            /**
             *  private List<Attrs> attrs;
             *
             *     @Data
             *     public static class Attrs{
             *         private Long attrId;
             *         private String attrName;
             *         private String attrValue;
             *     }
             */
            //设置检索属性
            esModel.setAttrs(attrsList);

            return esModel;
        }).collect(Collectors.toList());

        //TODO 5.将数据发送给es进行保存；发送给gulimall-search
        R r = searchFeignService.productStatusUp(upProducts);
        if(r.getCode() == 0){
            //远程调用成功
            //TODO 6.修改当前spu的状态
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }else {
            //远程调用失败
            //TODO 7.重复调用？接口幂等性；重试机制？
            //Feign调用流程
            /**
             * 1.构造请求数据，将对象转为JSON
             *      RequestTemplate template = buildTemplateFromArgs.create(argv);
             * 2.发送请求进行执行(执行成功会解码响应数据)
             *      executeAndDecode(template)
             * 3.执行请求会有重试机制,默认是关闭的
             *      while(true){
             *          try{
             *              executeAndDecode(template)
             *          }catch{ 没有执行成功会进行重试
             *              try{ retryer.continueOrPropagate(e);//重试器 } catch{ throw ex;   有异常抛出去 }
             *              continue;   没有异常会执行continue
             *          }
             *      }
             */
        }

    }

    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {
        //先sku_info表通过skuId找出它的spuId,再去spu_info表找到真正的spu信息
        SkuInfoEntity byId = skuInfoService.getById(skuId);
        Long spuId = byId.getSpuId();   //找到spu的id
        SpuInfoEntity spuInfoEntity = getById(spuId);//spu信息
        return spuInfoEntity;
    }

}






