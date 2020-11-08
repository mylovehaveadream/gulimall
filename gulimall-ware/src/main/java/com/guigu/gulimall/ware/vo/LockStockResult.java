package com.guigu.gulimall.ware.vo;

import lombok.Data;

//库存的锁定结果
@Data
public class LockStockResult {
    private Long skuId; //哪个商品
    private Integer num; //锁了几件
    private Boolean locked; //是否被锁定成功了
}
