package com.guigu.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {
    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 定制RabbitTemplate
     * 一、服务器收到消息就回调
     *      1.spring.rabbitmq.publisher-confirms=true
     *      2.设置确认回调 ConfirmCallback()
     *
     * 二、消息正确抵达队列进行回调
     *      1.
     *      #开启发送端消息抵达队列的确认
     *      spring.rabbitmq.publisher-returns=true
     *      #只要抵达队列，以异步方式优先回调我们这个returnconfirm
     *      spring.rabbitmq.template.mandatory=true
     *
     *      2.设置确认回调setReturnCallback
     *
     * 三、消费端确认（保证每个消息被正确消费，此时才可以broker删除这个消息）
     * spring.rabbitmq.listener.simple.acknowledge-mode=manual 手动签收
     *      1.默认是自动确认的，自动进行回复，只要消息接收到，客户端会自动确认，服务端就会移除这个消息
     *          问题：
     *              我们收到很多消息，自动回复给服务器ack，只有一个消息处理成功，宕机了，发生消息丢失
     *              消费者手动确认模式。处理一个就确认一个，没有确认的消息都不能进行删除。
     *                  只要我们没有明确告诉MQ，货物被签收，没有ack,消息就一直是unacked状态，
     *                  即使Consumer宕机，消息不会丢失，会重新变为Ready状态，下一次有新的Consumer连接进来就发给他
     *
     *      2.如何签收
     *          channel.basicAck(deliveryTag, false);  签收：业务成功完成就应该签收
     *          channel.basicNack(deliveryTag,false,true);  拒签：业务失败，拒签
     */
    @PostConstruct  //MyRabbitConfig对象创建完成以后，执行这个方法
    public void initRabbitTemplate(){
        //设置确认回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * @param correlationData:当前消息的唯一关联数据（这个是消息的唯一id）
             * @param b ack消息是否成功收到
             * @param s cause 失败的原因
             *
             * 代理收到消息，这个方法就会自动回调
             *
             * 1.只要消息能正确的抵达Broker服务器，这个回调的ack(b)就会是true
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                /**
                 * 1.做好消息确认机制（pulisher,consumer[手动ack]）
                 * 2.每一个发送的消息都在数据库做好记录，定期将失败的消息再次发送一遍
                 */
                //服务器收到了
                //修改消息的状态
                System.out.println("CorrelationData:" + correlationData + "->ack:" + b + "->cause" + s);
            }
        });

        //设置消息抵达队列的确认回调
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             * 触发时机，只要消息没有投递给指定的队列，就触发这个失败回调
             * @param message 投递失败的消息的详细信息
             * @param i replyCode 回复的状态码
             * @param s replyText 回复的文本内容
             * @param s1  exchange 当时这个消息发给那个交换机
             * @param s2  routingKey 当时这个消息用哪个路由键
             */
            @Override
            public void returnedMessage(Message message, int i, String s, String s1, String s2) {
                //报错误了，修改数据库当前消息的状态（日志表）->错误
                System.out.println("Fail Message:" + message + "->replyCode:" + i +
                        "->replyText:" + s + "->exchange:" + s1 + "->routingKey:" + s2);
            }
        });
    }


    /**
     *  使用JSON序列化机制，进行消息转换
     */
    @Bean
    public MessageConverter messageConverter(){ //容器中有，就用容器中的
        return new Jackson2JsonMessageConverter();
    }

}
