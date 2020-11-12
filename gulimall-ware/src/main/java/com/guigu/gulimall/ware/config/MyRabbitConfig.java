package com.guigu.gulimall.ware.config;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyRabbitConfig {
    /**
     *  使用JSON序列化机制，进行消息转换
     */
    @Bean
    public MessageConverter messageConverter(){ //容器中有，就用容器中的
        return new Jackson2JsonMessageConverter();
    }

    //第一次需要监听消息的时候，MQ发现Exchange等下面的东西都没有就会创建出来，不然就不会创建出来,所以要加上这一段的代码
//    @RabbitListener(queues = "stock.release.stock.queue")
//    public void handle( Message message) {}

    //库存服务的交换机
    @Bean
    public Exchange stockEventExchange(){
        //TopicExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments)
        return new TopicExchange("stock-event-exchange",true,false);
    }


    @Bean
    public Queue stockReleaseStockQueue() {
        Queue queue = new Queue("stock.release.stock.queue", true, false, false);
        return queue;
    }

    @Bean   //给容器中一放，会自动连上RabbitMQ，然后创建出来
    public Queue stockDelayQueue() {
        Map<String,Object> arguments = new HashMap<>();
        /**
         * x-dead-letter-exchange:stock-event-exchange
         * x-dead-letter-routing-key:stock.release
         * x-message-ttl:120000ms 2分钟
         */
        arguments.put("x-dead-letter-exchange","stock-event-exchange");
        arguments.put("x-dead-letter-routing-key","stock.release");
        arguments.put("x-message-ttl",120000);

        //Queue(String name(队列名),
        // boolean durable(持久化), boolean exclusive(排他,不是，就所有人都能连),
        // boolean autoDelete(自动删除), @Nullable Map<String, Object> arguments(自定义属性))
        Queue queue = new Queue("stock.delay.queue", true, false, false,arguments);
        return queue;
    }


    @Bean
    public Binding stockReleaseBinding(){
        //Binding(String destination(目的地),
        // Binding.DestinationType destinationType(目的地类型),
        // String exchange(哪个交换机), String routingKey(路由键), @Nullable Map<String, Object> arguments)
        return new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE,"stock-event-exchange",
                "stock.release.#",null);
    }

    @Bean
    public Binding stockLockedBinding(){
        return new Binding("stock.delay.queue",
                Binding.DestinationType.QUEUE,"stock-event-exchange",
                "stock.locked",null);
    }

}
