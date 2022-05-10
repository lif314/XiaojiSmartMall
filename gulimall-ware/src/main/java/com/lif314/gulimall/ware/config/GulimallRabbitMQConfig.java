package com.lif314.gulimall.ware.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class GulimallRabbitMQConfig {

    // 将发送接受消息转化为json数据
    @Bean
    public MessageConverter messageConverter(){
        // 配置序列化器
        return new Jackson2JsonMessageConverter();
    }

     // RabbitMQ只有在监听消息时才会自动创建交换机和消息队列。
//    @RabbitListener(queues = "stock.release.stock.queue")
//    public void handle(Message message){
//        System.out.println(message);
//    }


    /**
     * 交换机 库存服务默认交换机
     * Topic，可以绑定多个队列
     */
    @Bean
    public Exchange stockEventExchange() {
        //String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
        return new TopicExchange(
                "stock-event-exchange",
                true,
                false);
    }

    /**
     * 死信队列
     * 释放库存
     */
    @Bean
    public Queue stockReleaseStockQueue() {
        //String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments
        return new Queue(
                "stock.release.stock.queue",
                true,
                false,
                false);
    }

    /**
     * 延时队列
     * 锁定库存
     */
    @Bean
    public Queue stockDelayQueue() {
        HashMap<String, Object> arguments = new HashMap<>();
        // 信死了交给库存交换机
        arguments.put("x-dead-letter-exchange", "stock-event-exchange");
        arguments.put("x-dead-letter-routing-key", "stock.release");
        // 消息过期时间 1.5分钟
        arguments.put("x-message-ttl", 90000);
        return new Queue(
                "stock.delay.queue",
                true,
                false,
                false,
                arguments);
    }

    /**
     * 绑定：交换机与延时队列
     * 锁定库存
     */
    @Bean
    public Binding stockLockedBinding() {
        return new Binding(
                "stock.delay.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked",
                null);
    }


    /**
     * 绑定：交换机与死信队列
     * 释放库存
     */
    @Bean
    public Binding stockReleaseBinding() {
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.release.#",
                null);
    }

}
