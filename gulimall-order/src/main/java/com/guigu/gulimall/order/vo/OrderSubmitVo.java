package com.guigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交的数据
 */
@Data
public class OrderSubmitVo {

    private Long addrId;      //收货地址的id
    private Integer payType;  //支付方式
    //无需提交需要购买的商品，去购物车再获取一遍选中的商品，京东是这样的
    //优惠，发票

    private String orderToken;  //防重令牌
    private BigDecimal payPrice;    //应付价格 验价（提交订单的价格payPrice与自己订单算一个价格两个来对比，如两个不一样，进行提醒用户）
    private String note;    //订单备注

    //用户相关信息，直接去session取出登录的用户
}
