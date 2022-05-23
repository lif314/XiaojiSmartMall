package com.lif314.gulimall.order.intercepter;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lif314.common.constant.AuthServerConstant;
import com.lif314.common.to.MemberRespTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 用户登录拦截器
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    // 使用ThreadLocal共享用户数据
    public static ThreadLocal<MemberRespTo> loginUser = new ThreadLocal<>();
    /**
     * 预处理
     * @param request 请求
     * @param response 响应
     * @param handler 处理
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 解决远程调用需要登录问题--匹配路由直接放行
        // /order/order/status/{orderSn}
        String requestURI = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/order/order/status/**", requestURI);
        boolean match1 = antPathMatcher.match("/payed/notify", requestURI);
        if(match1 || match){
            return true;
        }

        String token = request.getHeader("TOKEN");
        String key = AuthServerConstant.LOGIN_USER + token;
        String s = redisTemplate.opsForValue().get(key);
        MemberRespTo memberRespTo = JSON.parseObject(s, new TypeReference<MemberRespTo>() {
        });
        if(memberRespTo != null){
            // 登录
            loginUser.set(memberRespTo);  // 共享用户数据
            return true;
        }else{
            return false;
        }
    }
}
