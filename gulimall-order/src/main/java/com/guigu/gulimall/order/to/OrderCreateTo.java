package com.guigu.gulimall.order.to;

import com.guigu.gulimall.order.entity.OrderEntity;
import com.guigu.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

//创建订单数据
@Data
public class OrderCreateTo {
    private OrderEntity order;  //订单实体类

    private List<OrderItemEntity> orderItems;    //订单项

    private BigDecimal payPrice;    //订单计算的应付价格

    private BigDecimal fare;    //运费
}
