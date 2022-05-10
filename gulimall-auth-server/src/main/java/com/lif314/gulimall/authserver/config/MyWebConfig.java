package com.lif314.gulimall.authserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebConfig implements WebMvcConfigurer {

    /**
     * 定制SpringMVC功能: 视图控制器
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry){
        /**
         *     @GetMapping("/login")
         *     public String loginPage(){
         *         return "login";
         *     }
         */
        registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("/register.html").setViewName("register");

    }
}
