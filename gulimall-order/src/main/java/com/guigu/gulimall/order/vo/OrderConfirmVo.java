package com.guigu.gulimall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

//订单确认页需要用的数据
public class OrderConfirmVo {

    //收货地址，ums_member_receive_address表
    @Setter @Getter
    List<MemberAddressVo> address;

    //所有选中的购物项
    @Setter @Getter
    List<OrderItemVo> items;

    //订单的防重令牌，防重复提交的
    @Setter @Getter
    String orderToken;

    //发票记录...

    //优惠卷信息...
    @Setter @Getter
    Integer integration;

    //库存信息，k->skuId,v->是否有库存
    @Setter @Getter
    Map<Long,Boolean> stocks;

    //有几件商品
    public Integer getCount(){
        Integer i = 0;
        if(items != null) {
            for (OrderItemVo item : items) {
                i += item.getCount();
            }
        }
        return i;
    }

//    BigDecimal total;
    // 订单总额
    public BigDecimal getTotal(){
        BigDecimal sum = new BigDecimal("0");
        if(items != null) {
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                sum = sum.add(multiply);
            }
        }
        return sum;
    }


//    BigDecimal payPrice;
    //应付价格
    public BigDecimal getPayPrice() {
       return getTotal();
    }
}
