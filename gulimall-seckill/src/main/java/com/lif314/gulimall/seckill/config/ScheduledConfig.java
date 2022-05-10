package com.lif314.gulimall.seckill.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync // 开启异步执行
@EnableScheduling // 开启调度
@Configuration
public class ScheduledConfig {
}
