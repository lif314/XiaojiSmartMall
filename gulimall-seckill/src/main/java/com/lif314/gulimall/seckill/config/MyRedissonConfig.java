package com.lif314.gulimall.seckill.config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;


@Configuration
public class MyRedissonConfig {
    /**
     * 程序化配置 -- 注入Redisson客户端实例对象
     *
     * 所有对Redisson的使用都是通过RedissonClient对象
      */
    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson(@Value("${spring.redis.host}") String host, @Value("${spring.redis.port}")String port, @Value("${spring.redis.password}") String password) throws IOException {
        // 1.创建配置
        Config config = new Config();
        // 单节点模式 -- 设置地址
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        config.useSingleServer().setPassword(password);
//        config.useSingleServer().setAddress("rediss://" + host + ":" + port);// 使用安全连接
//        config.useClusterServers().addNodeAddress("127.0.0.1:7004", "127.0.0.1:7001");// 集群模式
        // 2.创建redisson客户端实例
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }

}
