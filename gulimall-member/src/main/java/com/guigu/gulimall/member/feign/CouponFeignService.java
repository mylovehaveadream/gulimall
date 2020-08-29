package com.guigu.gulimall.member.feign;

import com.guigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

//@FeignClient("服务名")告诉SpringCloud这个接口是远程客户端
//调用gulimall-coupon远程服务
//这是一个声明式的远程调用
@FeignClient("gulimall-coupon")
public interface CouponFeignService {
    //调用远程的功能，把远程服务的请求和方法往这一复制就行了
    //路径要写全
    //这句话的意思：以后调用接口的这个方法，就会去注册中心中先找远程服务gulimall-coupon所在的位置，
    //再去调用这个请求/coupon/coupon/member/list对应的方法
    @RequestMapping("/coupon/coupon/member/list")
    public R membercoupons();
}
