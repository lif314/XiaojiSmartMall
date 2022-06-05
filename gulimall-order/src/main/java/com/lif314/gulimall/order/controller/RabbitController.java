package com.lif314.gulimall.order.controller;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RabbitController {

//    @Autowired
//    RabbitTemplate rabbitTemplate;
//    public String sendMsg(Integer num){
//        for (int i = 0; i < 10; i++) {
//            rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", i);
//        }
//        return null;
//    }



}
