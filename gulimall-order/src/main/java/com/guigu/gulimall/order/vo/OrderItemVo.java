package com.guigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {
    private Long skuId;
    private String title;
    private String image;
    private List<String> skuAttr;   //套餐信息
    private BigDecimal price;   //商品价格
    private Integer count;
    private BigDecimal totalPrice;  //总价

    private BigDecimal weight;  //商品的重量
}
