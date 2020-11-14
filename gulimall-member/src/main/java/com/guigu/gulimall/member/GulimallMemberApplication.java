package com.guigu.gulimall.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;


/**
 * 1.想要远程调用别的服务
 * 1）.引入open-feign，open-feign是声明式的远程调用
 * 2）.编写一个接口，告诉SpringCloud这个接口需要调用远程服务
 *  1.声明接口的每一个方法都是调用哪个远程服务的哪个请求
 * 3）.开启远程调用功能@EnableFeignClients
 *  basePackages,远程调用的接口包，只要服务一启动，就会自动扫描这个包下所有标了
 *  @FeignClient注解的接口
 */
@EnableRedisHttpSession
@EnableFeignClients(basePackages = "com.guigu.gulimall.member.feign")
@EnableDiscoveryClient
@SpringBootApplication
public class GulimallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallMemberApplication.class, args);
    }

}
