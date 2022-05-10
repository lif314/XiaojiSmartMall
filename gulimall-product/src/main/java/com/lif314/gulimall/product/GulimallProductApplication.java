package com.lif314.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@EnableRedisHttpSession  // 开启Redis存储Session
@EnableDiscoveryClient
@MapperScan("com.lif314.gulimall.product.dao")
@SpringBootApplication
@EnableFeignClients(basePackages = "com.lif314.gulimall.product.feign") // 开启远程调用功能，可以添加扫描包的地址,需要父子同包
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
