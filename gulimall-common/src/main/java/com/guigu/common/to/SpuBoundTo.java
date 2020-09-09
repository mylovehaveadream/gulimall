package com.guigu.common.to;

import lombok.Data;

import java.math.BigDecimal;

//当A服务给B服务发数据，可以将数据封装成一个对象
//在整个传输期间，数据模型可以称为一个To
//A和B都要用的，To可以放在common中
@Data
public class SpuBoundTo {

    private Long spuId;
    private BigDecimal buyBounds;
    private BigDecimal growBounds;
}
