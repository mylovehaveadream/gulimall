package com.guigu.gulimall.order.service.impl;

import com.guigu.gulimall.order.entity.OrderEntity;
import com.guigu.gulimall.order.entity.OrderReturnReasonEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guigu.common.utils.PageUtils;
import com.guigu.common.utils.Query;

import com.guigu.gulimall.order.dao.OrderItemDao;
import com.guigu.gulimall.order.entity.OrderItemEntity;
import com.guigu.gulimall.order.service.OrderItemService;


@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }


    /**
     * queues:声明需要监听的所有队列
     *  org.springframework.amqp.core.Message;
     *
     *  参数可以写以下的类型
     *  1.Message message：原生消息的详细信息。头+体
     *  2.T<发送的消息的类型，当时发的是什么类型的消息，就可以写什么类型> OrderReturnReasonEntity content，不用手动的进行转化，自动的转
     *  3.Channel channel:一个客户端只会建立一条链接，数据在通道进行传输，当前传输数据的通道
     *
     *  Queue:可以有很多人都来监听。只要收到消息，队列就会删除消息，而且只能有一个收到此消息
     *  场景：
     *      1.订单服务启动多个，同一个消息，只能有一个客户端收到，竞争关系
     *      2.只有一个消息完全处理完，方法运行结束，我们就可以接收到下一个消息
     */
//    @RabbitListener(queues = {"hello-java-queue"})   //标注在业务逻辑组件上，而组件必须在容器中，注解才能起作用
    @RabbitHandler //这个来接收消息，当@RabbitListener在类上时，不做方法上时，这样可以接收不同类型的消息
    public void recieveMessage(Message message,
                               OrderReturnReasonEntity content,
                               Channel channel) throws InterruptedException {
        //{"id":1,"name":"哈哈","sort":null,"status":null,"createTime":1581144531744}
        byte[] body = message.getBody();    //消息体，对应的是JSON的数据
        MessageProperties properties = message.getMessageProperties();  //消息头的属性信息
//        System.out.println("接收到消息...内容：" + message + "->类型：" + message.getClass());
        System.out.println("接收到消息...：" + message + "->内容：" + content);
        Thread.sleep(3000);
        System.out.println("消息出来完成=>" +content.getName());

        //channel内按顺序自增的
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        //签收货物，非批量模式；消费者一连进来，消息都会交给通道channel里面
        try {
            //只有手工签收了的货物才可以在消息队列里面删除
            if(deliveryTag % 2 == 0) {
                //收货
                channel.basicAck(deliveryTag, false);    //false只签收当前的货物（做一个确认一个），true就是批量签收的模式
                System.out.println("签收货物");
            } else {
                //退货 requeue=false 丢弃   requeue=true 发回服务器，服务器重新入队，再重新发出
                //basicNack(long var1(deliveryTag), boolean var3(multiple), boolean var4(requeue,是否重新入队))
                channel.basicNack(deliveryTag,false,true);
                //basicReject(long var1(deliveryTag), boolean var3(requeue))
//                channel.basicReject();
                System.out.println("没有签收货物");
            }
        } catch (IOException e) {
            //网络中断
            e.printStackTrace();
        }
    }

    //@RabbitHandler:标注了各种不同的接消息的方法，这样可以接收不同类型的消息
    @RabbitHandler //这个来接收消息，当@RabbitListener在类上时，不在方法上时
    public void recieveMessage2(OrderEntity content) throws InterruptedException {
        //{"id":1,"name":"哈哈","sort":null,"status":null,"createTime":1581144531744}

        System.out.println("接收到消息...：" + content);
    }
}