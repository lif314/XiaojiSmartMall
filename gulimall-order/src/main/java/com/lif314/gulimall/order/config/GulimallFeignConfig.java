package com.lif314.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Feign自定义拦截器：解决远程调用丢失请求从中的cookie信息
 *  1、传用户id
 *  2、传用户会话 -- 更安全
 */
@Configuration
public class GulimallFeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor(){
      return new RequestInterceptor(){
          @Override
          public void apply(RequestTemplate requestTemplate) {
//              System.out.println("Feign在远程之前构造模板：" + requestTemplate);
              // 加上原来的请求头
              // 可以使用RequestContextHolder.getRequestAttributes()拿到刚进来的请求
              ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
              if(attributes != null){
                  // 获取老请求
                  HttpServletRequest request = attributes.getRequest();
                  // 给新请求同步老请求的cookie
                  String cookie = request.getHeader("Cookie");
                  requestTemplate.header("Cookie", cookie);
              }
          }
      };
    }

}
