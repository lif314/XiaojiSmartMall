package com.lif314.gulimall.order;

import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.Serializable;

@SpringBootTest
class GulimallOrderApplicationTests {

    @Test
    void contextLoads() {
    }

    /**
     * 测试使用RabbitMQ:
     */

    // 1、如何创建Exchange Queue Binding --- AmqpAdmin

    @Autowired
    AmqpAdmin amqpAdmin;
    @Test
    public void createExchange(){
        // 创建直接交换机
        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
        System.out.println("直接交换机：" + directExchange);
    }

    @Test
    public void createQueue(){
        // 创建队列
        Queue queue = new Queue("hello-java-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
        System.out.println("队列：" + queue);
    }

    @Test
    public void createBinding(){
        Binding binding = new Binding("hello-java-queue", Binding.DestinationType.QUEUE,"hello-java-exchange", "hello.java", null);
        amqpAdmin.declareBinding(binding);
        System.out.println("绑定关系：" + binding);
    }



    // 如何收发消息
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Test
    public void sendMsg(){
        rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", "Hello Java");
    }

    // 发送对象
    @Data
    public static class User implements Serializable {
        private String name;
        private Integer age;
    }

    @Test
    public void sendObjMsg(){
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setName(i+":llf");
            user.setAge(11);
            rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", user);
        }

    }









}
