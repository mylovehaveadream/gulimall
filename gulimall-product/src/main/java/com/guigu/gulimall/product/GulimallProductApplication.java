package com.guigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 1.整合mybatis-plus
 *      1）.导入依赖
 *       <!--使用mybatis-plus-->
 *        <dependency>
 *            <groupId>com.baomidou</groupId>
 *            <artifactId>mybatis-plus-boot-starter</artifactId>
 *            <version>3.2.0</version>
 *        </dependency>
 *      2）.配置
 *          1.配置数据源
 *              1）.导入数据库驱动，将他写在common里面
 *              <dependency>
 *                  <groupId>mysql</groupId>
 *                  <artifactId>mysql-connector-java</artifactId>
 *                  最好是8.0版本的
 *                  <version>5.1.37</version>
 *              </dependency>
 *              2）.在application.yml配置数据源相关信息
 *          2.配置mybatis-plus
 *              1）.使用MapperScan扫描接口
 *              2）.告诉mybatis-plus，SQL的映射文件位置
 *
 */

@EnableRedisHttpSession
//如果父子不同包，就要显示的声明路径
@EnableFeignClients(basePackages = "com.guigu.gulimall.product.feign")
@EnableDiscoveryClient
@MapperScan("com.guigu.gulimall.product.dao")
@SpringBootApplication
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
