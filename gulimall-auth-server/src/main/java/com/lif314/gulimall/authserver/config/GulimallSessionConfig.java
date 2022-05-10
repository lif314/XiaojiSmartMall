package com.lif314.gulimall.authserver.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * 自定义SpringSession配置
 */
@Configuration
public class GulimallSessionConfig {

    @Bean
    public CookieSerializer cookieSerializer(){
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        // 设置作用域
        cookieSerializer.setDomainName("feihong.com");
        // 设置名字
        cookieSerializer.setCookieName("GULISESSION");
        return cookieSerializer;
    }

    // 序列化器
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer(){
        return new GenericJackson2JsonRedisSerializer(); // 序列化器
        //return new GenericFastJsonRedisSerializer(); // 序列化器
    }

}
