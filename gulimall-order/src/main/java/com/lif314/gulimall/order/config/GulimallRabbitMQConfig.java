package com.lif314.gulimall.order.config;



import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class GulimallRabbitMQConfig {

//    @Autowired
//    RabbitTemplate rabbitTemplate;

    // 将发送接受消息转化为json数据
    @Bean
    public MessageConverter messageConverter(){
        // 配置序列化器
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制 RabbitTemplate
     * 1、服务端收到消息进行回调
     *     1、spring.rabbitmq.publisher-confirm-type=correlated
     *     2、设置回调ConfirmCallback
     *
     * 2、消息是否抵达队列进行回调
     *      1、spring.rabbitmq.publisher-returns=true
     *          #只要抵达队列，以异步优先发送回调我们这个returns
     *          spring.rabbitmq.template.mandatory=true
     *      2、setReturnsCallback
     *  3、消费者确认Ack
     *      1、默认是自动确认的，只要消息收到，客户端会自动确认，服务端就会一处这个消息
     *      2、问题：收到很多消息，自动恢复给服务器ack，只有一个消息处理完，
     *            发现消息丢失
     *        ===> 手动确认
     *      3、手动确认
     *          （1）spring.rabbitmq.listener.simple.acknowledge-mode=manual
     *          （2）没有明确恢复，即使宕机，消息也不会丢失。保证消费者货物不会丢失
     *          （3）手动确认: 使用Channel channel确认消息接受
     */
//    @PostConstruct  // 在对象创建完后执行该方法
//    public void initRabbitTemplate() {
//        // 设置确认返回
//        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
//            /**
//             * @param correlationData 消息id,当前消息的唯一关联的数据
//             * @param ack             消息是否成功收到
//             * @param cause           失败的原因
//             */
//            @Override  // 服务器收到了
//            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
//                log.info("confirm...\ncorrelationData[" + correlationData +
//                        "]===>ack[" + ack + "]==>cause["
//                        + cause + "]");
//            }
//        });
//
//        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
//
//            /**
//             * 只有消息没有正确抵达队列才会回调
//             */
//            @Override  // 报错
//            public void returnedMessage(ReturnedMessage returned) {
//                Message message = returned.getMessage();
//                int replyCode = returned.getReplyCode();
//                String exchange = returned.getExchange();
//                String replyText = returned.getReplyText();
//                String routingKey = returned.getRoutingKey();
//                log.error("消息抵达队列错误信息：" + message
//                        +"\n状态码：" + replyCode
//                        +"\n交换机：" + exchange
//                        +"\n返回文本："  + replyText
//                        +"\n路由键： " + routingKey);
//            }
//        });
//    }
}
