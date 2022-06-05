package com.lif314.gulimall.ware.listener;

import com.lif314.gulimall.ware.to.StockLockedTo;
import com.lif314.gulimall.ware.service.WareSkuService;
import com.lif314.gulimall.ware.vo.OrderEntityVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RabbitListener(queues = "stock.release.stock.queue") // 监听库存解锁队列
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;
    /**
     *
     *
     * 下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚，之前锁定的
     *  库存都要自动解锁
     */
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {

        System.out.println(">>>> 收到解锁库存消息......." + to.getOrderSn());
//        StockLockedTo to = JSON.parseObject(s, new TypeReference<StockLockedTo>(){});

        try{
            wareSkuService.handleUnLockStockWare(to);
            // 消息消费成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            // 消息消费失败
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }


    /**
     * 处理订单关闭消息
     */
    @RabbitHandler
    public void handleOrderCloseRelease(OrderEntityVo vo, Message message, Channel channel) throws IOException {

//        String s = JSON.toJSONString(vo);
//        OrderEntityVo vo = JSON.parseObject(s, new TypeReference<OrderEntityVo>(){});

        System.out.println("订单关闭，准备解锁库存...." + vo.getOrderSn());
        try {
            wareSkuService.handleUnLockStockOrder(vo);
            // 走到这里宕机，消息重复发送  -- 设置为幂等的   防重表
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
