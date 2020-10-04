package com.guigu.gulimall.ware.vo;

import lombok.Data;

@Data
public class SkuHasStockVo {

    private Long skuId;
    private Boolean hasStock;   //是否有库存
}
