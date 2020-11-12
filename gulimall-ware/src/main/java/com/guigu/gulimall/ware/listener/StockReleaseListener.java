package com.guigu.gulimall.ware.listener;


import com.guigu.common.to.mq.OrderTo;
import com.guigu.common.to.mq.StockLockedTo;
import com.guigu.gulimall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {
    @Autowired
    WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存的消息...");
        try {
            //当前消息是否被第二次及以后（重新）派发过来了，这样太暴力了
            //Boolean redelivered = message.getMessageProperties().getRedelivered();
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e){
            //消息拒绝以后重新放到队列里面，让别人继续消费解锁
            //有任何异常，重新回队
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("订单关闭准备解锁库存...");
        try {
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e){
            //消息拒绝以后重新放到队列里面，让别人继续消费解锁
            //有任何异常，重新回队
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }
}
