package com.guigu.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
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
 */
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
