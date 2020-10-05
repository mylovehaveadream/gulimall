package com.guigu.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class MyRedissonConfig {

    //所有对Redisson的使用都是通过RedissonClient对象
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() throws IOException {
        //单节点模式
        //1.创建配置
        Config config = new Config();
//        Redis url should start with redis:// or rediss://
        config.useSingleServer().setAddress("redis://47.113.95.188:6379");

        //2.根据Config创建出RedissonClient实例
        RedissonClient redissonClient = Redisson.create(config);
        return  redissonClient;
    }

}
