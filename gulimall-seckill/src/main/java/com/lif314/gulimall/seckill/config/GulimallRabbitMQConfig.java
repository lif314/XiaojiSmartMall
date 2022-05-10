package com.lif314.gulimall.seckill.config;



import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GulimallRabbitMQConfig {

    // 将发送接受消息转化为json数据
    @Bean
    public MessageConverter messageConverter(){
        // 配置序列化器
        return new Jackson2JsonMessageConverter();
    }
}
