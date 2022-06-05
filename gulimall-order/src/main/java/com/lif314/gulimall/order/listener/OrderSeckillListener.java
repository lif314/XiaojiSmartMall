package com.lif314.gulimall.order.listener;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lif314.common.to.mq.SeckillOrderTo;
import com.lif314.gulimall.order.entity.OrderEntity;
import com.lif314.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RabbitListener(queues = "order.seckill.order.queue")
public class OrderSeckillListener {

    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void SeckillOrderListener(SeckillOrderTo to, Channel channel, Message message) throws IOException {

        String s = JSON.toJSONString(to);
        System.out.println("秒杀订单：" + s);
        SeckillOrderTo seckillOrder = JSON.parseObject(s, new TypeReference<SeckillOrderTo>(){});

        try{
            // 创建秒杀订单
            orderService.cereateSeckillOrder(seckillOrder);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        }catch (Exception e){
            // 关闭订单失败，重新回到队列
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
