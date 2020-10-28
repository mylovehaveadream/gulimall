package com.guigu.gulimall.product.vo;

import com.guigu.gulimall.product.entity.SkuImagesEntity;
import com.guigu.gulimall.product.entity.SkuInfoEntity;
import com.guigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    //1.sku的基本信息的获取 pms_sku_info
    SkuInfoEntity info;

    //是否有货,不来查询，默认是有货的
    boolean hasStock = true;

    //2.sku的图片信息  pms_sku_images
    List<SkuImagesEntity> images;

    //3.获取spu的销售属性组合
    List<SkuItemSaleAttrVo> saleAttr;

    //4.获取spu的介绍 pms_spu_info_desc
    SpuInfoDescEntity desp;

    //5.获取spu的规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;


//    @Data
//    public static class SkuItemSaleAttrVo{
//        private Long attrId;
//        private String attrName;
//        private List<String> attrValues;
//    }


//    @Data
//    public static class SpuItemAttrGroupVo {
//        private String groupName;   //分组的名字
//        private List<SpuBaseAttrVo>  attrs;   //基本属性
//    }

//    @Data
//    public static class SpuBaseAttrVo { //基本属性的类
//        private String attrName;
//        private String attrValue;
//    }
}
