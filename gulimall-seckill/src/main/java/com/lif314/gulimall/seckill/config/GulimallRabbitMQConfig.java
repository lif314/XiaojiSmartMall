package com.lif314.gulimall.seckill.config;



import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GulimallRabbitMQConfig implements InitializingBean {
    @Autowired
    RabbitTemplate rabbitTemplate;

    // 将发送接受消息转化为json数据
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper){
        // 配置序列化器
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //使用JSON序列化
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    }
}
