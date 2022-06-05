package com.lif314.gulimall.cart.interceptor;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lif314.common.constant.AuthServerConstant;
import com.lif314.common.constant.CartConstant;
import com.lif314.common.to.MemberRespTo;
import com.lif314.gulimall.cart.to.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 拦截器: 在执行目标方法之前，先判断用户的登录状态，
 * 并封装传递给controller目标请求
 */
@Component  // 拦截器是一个组件
public class CartInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // ThreadLocal 同一线程之间共享数据 --- Map(线程号, 共享的数据)
    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    /**
     * 在目标方法执行之前执行
     *
     * @param request  请求
     * @param response 响应
     * @param handler  执行方法
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        // 解决远程调用需要登录问题--匹配路由直接放行

//        String requestURI = request.getRequestURI();
////        System.out.println(requestURI);
//        AntPathMatcher antPathMatcher = new AntPathMatcher();
//        boolean match = antPathMatcher.match("/cart/currentUserCartItems", requestURI);
//        if(match){
//            return true;
//        }

        // 获取Session,从Session中获取当前登录用户
        UserInfoTo userInfoTo = new UserInfoTo();

        String token = request.getHeader("TOKEN");
//        System.out.println(token);
        String key = AuthServerConstant.LOGIN_USER + token;
        String s = redisTemplate.opsForValue().get(key);
        if(!StringUtils.isEmpty(s)){
            // 用户已经登陆
            MemberRespTo member = JSON.parseObject(s, new TypeReference<MemberRespTo>() {
            });
            userInfoTo.setUserId(member.getId());
        }
        // 没有登录,创建临时用户，查看临时购物车
        // 从cookie中获取信息
        Cookie[] cookies = request.getCookies();
        if(cookies != null && cookies.length > 0){
            for (Cookie cookie : cookies) {
                //  user-key
                String name = cookie.getName();
                if(name.equals(CartConstant.TEMP_USER_COOKIE_NAME)){
                    // 该临时用户已经存在--获取临时用户信息
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setTempUser(true);
                }
            }
        }
        // 如果没有临时用户，则创建一个
        if(StringUtils.isEmpty(userInfoTo.getUserKey())){
            String userKey = UUID.randomUUID().toString();
            userInfoTo.setUserKey(userKey);
        }

        // 在目标方法执行之前，使用threadLocal.
        // 这样目标方法就可以快速获取用户信息
//        System.out.println("userinfo:"  + userInfoTo);
        threadLocal.set(userInfoTo);
        // 全部放行
        return true;
    }


    /**
     * 业务执行之后，命令浏览器保存一个cookie信息，该信息一个月后失效
     *
     * 分配临时用户
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

        UserInfoTo userInfoTo = threadLocal.get();
        if(userInfoTo != null && !userInfoTo.isTempUser()){
            // 如果没有临时用户信息，则放在cookie中
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME,userInfoTo.getUserKey());
            // 设置cookie的作用域
//            cookie.setDomain("feihong.com");
            // 设置过期时间 -- 一个月
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            // 添加cookie
            response.addCookie(cookie);
        }
    }
}
