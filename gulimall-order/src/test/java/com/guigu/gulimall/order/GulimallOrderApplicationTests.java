package com.guigu.gulimall.order;

import com.guigu.gulimall.order.entity.OrderEntity;
import com.guigu.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;

@Slf4j
@SpringBootTest
class GulimallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    public void sendMessageTest() {

        //1.发送消息,把对象给发出去,如果发送的消息是个对象，我们会使用序列化机制，将对象写出去，对象必须实现Serializable
        String msg = "Hello World!";

        for(int i=0;i<10;i++){  //发出不同的对象
             if(i%2==0){
                 OrderReturnReasonEntity reasonEntity = new OrderReturnReasonEntity();
                 reasonEntity.setId(1L);
                 reasonEntity.setCreateTime(new Date());
                 reasonEntity.setName("gg");
                 //2.发送的对象类型的消息，可以是一个JSON
                 rabbitTemplate.convertAndSend("hello-java-exchange","hello.java", reasonEntity,
                    new CorrelationData(UUID.randomUUID().toString()));//转换并且发送，发给rabbitmq
                 // new CorrelationData(UUID.randomUUID().toString())代表消息的唯一id
                 log.info("消息发送完成",reasonEntity);
             }else {
                 OrderEntity entity = new OrderEntity();
                 entity.setOrderSn(UUID.randomUUID().toString());
                 rabbitTemplate.convertAndSend("hello-java-exchange","hello.java", entity,
                         new CorrelationData(UUID.randomUUID().toString()));
             }
        }
    }

    /**
     * 1.如何创建Exchange(hello-java-exchange)、Queue、Binding
     *      1.使用AmqpAdmin进行创建
     * 2.如何收发消息
     */
    @Test
    public void createExchange() {
        //amqpAdmin
        //Exchange
        /**
         * DirectExchange(String name(名字), boolean durable(是否持久化), boolean autoDelete(是否自动删除),
         *                 Map<String, Object> arguments)
         */
        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true,false);
        amqpAdmin.declareExchange(directExchange);    //声明交换机
        log.info("Exchange创建成功","hello-java-exchange");
    }


    @Test
    public void createQueue() {
        /**
         * Queue(String name, boolean durable, boolean exclusive(是否排它队列，让所有人都能连到队列，谁能接到消息只有一个可以的接收),
         *          boolean autoDelete, @Nullable Map<String, Object> arguments)
         */
        Queue queue = new Queue("hello-java-queue",true,false,false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue创建成功","hello-java-queue");
    }

    @Test
    public void createBinding(){
        /**
         * Binding(String destination[目的地(如：队列)], Binding.DestinationType destinationType[目的地类型],
         *              String exchange[交换机], String routingKey[路由键], @Nullable Map<String, Object> arguments[自定义参数])
         *
         *  将exchange指定的交换机和destination目的地进行绑定，使用routingKey作为指定的路由键
         */
        Binding binding = new Binding("hello-java-queue", Binding.DestinationType.QUEUE,
                "hello-java-exchange","hello.java",null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding创建成功","hello-java-binding");
    }

}
