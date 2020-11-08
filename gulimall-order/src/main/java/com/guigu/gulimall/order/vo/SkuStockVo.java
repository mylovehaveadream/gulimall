package com.guigu.gulimall.order.vo;

import lombok.Data;

@Data
public class SkuStockVo {
    private Long skuId;
    private Boolean hasStock;   //是否有库存
}
