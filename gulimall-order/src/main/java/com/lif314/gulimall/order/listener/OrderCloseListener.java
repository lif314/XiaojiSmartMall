package com.lif314.gulimall.order.listener;


import com.lif314.gulimall.order.entity.OrderEntity;
import com.lif314.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.IOException;


/**
 * 定时关单
 */
@Component
@RabbitListener(queues = "order.release.order.queue")
public class OrderCloseListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void closeOrderListener(OrderEntity orderEntity, Channel channel, Message message) throws IOException {

//        OrderEntity orderEntity = JSON.parseObject(order, new TypeReference<OrderEntity>(){});

        try{
            orderService.closeOrder(orderEntity);
            // 关单成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            // 支付宝手动收单
        }catch (Exception e){
            // 关闭订单失败，重新回到队列
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
