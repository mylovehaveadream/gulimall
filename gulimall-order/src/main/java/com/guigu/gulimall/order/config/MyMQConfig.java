package com.guigu.gulimall.order.config;

import com.guigu.gulimall.order.entity.OrderEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyMQConfig {

    //@Bean Binding（绑定）、Queue（队列）、Exchange（交换机）把这些放在容器中，默认就会生效

    //延时队列，死信队列
    /**
     *  容器中的 Binding（绑定）、Queue（队列）、Exchange（交换机） 都会自动的创建（前提RabbitMQ中没有的情况）
     *  RabbitMQ只要有这些，@Baen声明的属性发生变化，重新运行也不会覆盖
     */
    @Bean   //给容器中一放，会自动连上RabbitMQ，然后创建出来
    public Queue orderDelayQueue() {
        Map<String,Object> arguments = new HashMap<>();
        /**
         * x-dead-letter-exchange:order-event-exchange
         * x-dead-letter-routing-key:order.release.order
         * x-message-ttl:60000ms 1分钟
         */
        arguments.put("x-dead-letter-exchange","order-event-exchange");
        arguments.put("x-dead-letter-routing-key","order.release.order");
        arguments.put("x-message-ttl",60000);

        //Queue(String name(队列名),
        // boolean durable(持久化), boolean exclusive(排他),
        // boolean autoDelete(自动删除), @Nullable Map<String, Object> arguments(自定义属性))
        Queue queue = new Queue("order.delay.queue", true, false, false,arguments);
        return queue;
    }

    @Bean
    public Queue orderReleaseOrderQueue() {
        Queue queue = new Queue("order.release.order.queue", true, false, false);
        return queue;
    }

    @Bean
    public Exchange orderEventExchange(){
        //TopicExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments)
        return new TopicExchange("order-event-exchange",true,false);
    }

    @Bean
    public Binding orderCreateOrderBinding(){
        //Binding(String destination(目的地),
        // Binding.DestinationType destinationType(目的地类型),
        // String exchange(哪个交换机), String routingKey(路由键), @Nullable Map<String, Object> arguments)
        return new Binding("order.delay.queue", Binding.DestinationType.QUEUE,"order-event-exchange",
                "order.create.order",null);
    }

    @Bean
    public Binding orderReleaseOrderBinding(){
        return new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,"order-event-exchange",
                "order.release.order",null);
    }


    /**
     * 订单释放直接和库存释放进行绑定
     */
    @Bean
    public Binding orderReleaseOtherBinding(){
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,"order-event-exchange",
                "order.release.order.#",null);
    }
}

















