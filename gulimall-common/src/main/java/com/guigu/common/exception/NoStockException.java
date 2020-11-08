package com.guigu.common.exception;

public class NoStockException extends RuntimeException{
    private Long skuId;

    public NoStockException(String msg) {
        super(msg + ";订单锁定失败");
    }

    public NoStockException(Long skuId) {
        super("商品id:" + skuId + ";没有足够的库存了");
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
}
