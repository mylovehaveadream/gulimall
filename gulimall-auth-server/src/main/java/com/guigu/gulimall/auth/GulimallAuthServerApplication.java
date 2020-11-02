package com.guigu.gulimall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 核心原理：
 * 1.@EnableRedisHttpSession导入RedisHttpSessionConfiguration配置
 *      1.给容器中添加了一个组件
 *          SessionRepository(存session的仓库) => [RedisIndexedSessionRepository（
 *                  RedisOperationsSessionRepository）]:redis操作session，session的增删改查封装类
 *      2.SessionRepositoryFilter ==> Filter：session存储过滤器,每个请求过来都必须经过filter
 *          1.创建的时候，就自动从容器中获取到SessionRepository
 *          2.原始的request、response都被包装了。SessionRepositoryRequestWrapper，SessionRepositoryResponseWrapper
 *          3.以后获取session。request.getSession()原生的；
 *          //SessionRepositoryRequestWrapper
 *          4.wrappedRequest.getSession();用这个来获取session ==> SessionRepository中获取到的session，也就是在redis中获取
 *
 * 原理是装饰者模式
 *
 * 也会对session进行自动延期；redis中的数据也是有过期时间的
 *
 */


//开启springSession的功能
@EnableRedisHttpSession //整合redis作为session的存储
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class GulimallAuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallAuthServerApplication.class, args);
    }

}
