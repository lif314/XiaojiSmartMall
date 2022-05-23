package com.lif314.gulimall.cart.config;


import com.lif314.gulimall.cart.interceptor.CartInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * 使用拦截器需要进行配置
 */
@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {

    @Autowired
    private CartInterceptor cartInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
       // 添加拦截器, 拦截路径-拦截任意请求
        // 就不用在放在容器中了
        registry.addInterceptor(this.cartInterceptor).addPathPatterns("/**");
    }
}
