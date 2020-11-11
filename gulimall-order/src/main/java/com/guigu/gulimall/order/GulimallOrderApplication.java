package com.guigu.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 使用RabbitMQ
 * 1.引入了amqp场景,RabbitAutoConfiguration就会自动生效
 * 2.给容器中自动配置了  RabbitTemplate、AmqpAdmin、CachingConnectionFactory、RabbitMessagingTemplate
 *      所有的属性都在这里面进行绑定的
 *      @ConfigurationProperties(prefix = "spring.rabbitmq")
 *      public class RabbitProperties
 *
 * 3.给配置文件中配置spring.rabbitmq信息
 * 4.@EnableRabbit：@Enablexxxx 开启功能
 * 5.监听消息：使用@RabbitListener,必须有@EnableRabbit，队列也必须存在
 *      @RabbitListener：标在类+方法上（监听那些队列即可）
 *      @RabbitHandler：标在方法上（重载区分不同的消息，和@RabbitListener搭配使用）
 *
 *
 * 本地事务失效
 * 同一个对象内事务互调默认失效，原因：绕过了代理对象，事务使用代理对象来控制的
 * 解决：使用代理对象来调用事务方法
 *  1.引入aop-starter:spring-boot-starter-aop,引入了aspectj
 *  2.@EnableAspectJAutoProxy(exposeProxy = true):开启aspectj动态代理功能，
 *      所有的动态代理都是aspectj创建的（即使没有接口也可以创建动态代理），而不是jdk代理的
 *      exposeProxy = true:对外暴露代理对象
 *  3.本类互调用代理对象
 *       OrderServiceImpl orderService = (OrderServiceImpl) AopContext.currentProxy();   //拿到当前代理对象
 *       //使用代理对象调的，下面的设置才有用
 *       orderService.b();
 *       orderService.c();
 *
 *
 * Seata控制分布式事务
 * 1.每一个微服务先必须创建undo_log表；
 * 2.安装事务协调器，seata-server: https://github.com/seata/seata/releases
 * 3.整合
 *  1.导入依赖spring-cloud-starter-alibaba-seata seata-all-0.9.0
 *  2.解压并启动seata-server服务器
 *      registry.conf:注册中心配置，seata-server也要注册到注册中心里面，修改registry type=nacos
 *      file.conf:用文件的方式来配置
 *  3.@GlobalTransactional 全局的事务，就能用到seata的事务了，在它的下面还要标上@Transactional
 *  4.所有想要用到分布式事务的微服务使用seata的DataSourceProxy代理自己的数据源
 *  5.每个微服务，都必须导入
 *      registry.conf
 *      file.conf：vgroup_mapping.{application.name}-fescar-service-group = "default"
 *  6.启动测试分布式事务
 *  7.给分布式大事务的入口标注@GlobalTransactional
 *  8.每一个远程的小事务用@Transactional
 *
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableFeignClients
@EnableRedisHttpSession
@EnableRabbit
@EnableDiscoveryClient
@SpringBootApplication
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
