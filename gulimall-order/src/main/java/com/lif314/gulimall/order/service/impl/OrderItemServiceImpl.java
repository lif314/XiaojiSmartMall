package com.lif314.gulimall.order.service.impl;

import com.rabbitmq.client.Channel;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.order.dao.OrderItemDao;
import com.lif314.gulimall.order.entity.OrderItemEntity;
import com.lif314.gulimall.order.service.OrderItemService;


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

    // 发送对象
    @Data
    public static class User implements Serializable {
        private String name;
        private Integer age;
    }

    /**
     * 测试接受消息
     * 监听消息@RabbitListener，必须使用@EnableRabbit,必须在容器中
     *
     * 参数可以写类型
     * - Message 原生消息的详细信息，有headers+body
     * - User 消息的本身类型，Spring会自动转换
     * - Channel channel 传输数据的通道
     *
     * Queue: 可以有多人在监听，队列删除消息，而且只能有一个收到消息
     * 测试结果：可以有多个在监听，但一个消息只能被其中一个服务收到
     *
     * 只有一个消息处理结束，才能接受下一个消息
     *
     * @RabbitListener 可以标注在类上
     * @RabbitHandler 只能在方法上标注
     *
     */
    @RabbitListener(queues = {"hello-java-queue"})  // queues是个数据，可以监听多个队列
    public void receiveMessage(Message message, User content, Channel channel)  {
//        byte[] body = message.getBody();
//        MessageProperties headers = message.getMessageProperties();
//        System.out.println("接受消息：" + message);
//        System.out.println("消息类型:"  + message.getClass());
        System.out.println("消息内容：" + content);
//        System.out.println("通道：" + channel);

        // channe中按序自增
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        // 签收货物
        try {
            // 防止网络中断
            channel.basicAck(deliveryTag, false); // 是否批量签收
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // 拒收模式
            // requeue=false丢弃消息   requeue=true 重新入队
            // basicNack(long deliveryTag, boolean multiple, boolean requeue)
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
