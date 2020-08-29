package com.guigu.gulimall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 1、开启服务注册发现@EnableDiscoveryClient
 *  网关将请求路由到哪其他服务，他就知道服务在哪了
 *  （配置nacos的注册中心地址）
 *  spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
 *  spring.application.name=gulimall-gateway
 */
@EnableDiscoveryClient
//网关暂时没有用到数据源，排除与数据源相关配置
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class GulimallGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallGatewayApplication.class, args);
    }

}
