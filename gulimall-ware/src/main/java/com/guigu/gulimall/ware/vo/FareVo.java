package com.guigu.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {
    private MemberAddressVo address;    //地址信息
    private BigDecimal fare;    //运费
}
