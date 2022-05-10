//package com.lif314.gulimall.authserver.controller;
//
//import com.lif314.common.utils.R;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// * token控制
// */
//
//@RestController
//public class TokenController {
//
//    @Autowired
//    private TokenService tokenService;
//
//    @Autowired
//    private SysLoginService sysLoginService;
//
//    @PostMapping("/login")
//    public R<?> login(@RequestBody LoginBody form)
//    {
//        // 用户登录
//        LoginUser userInfo = sysLoginService.login(form.getUsername(), form.getPassword());
//        // 获取登录token
//        return R.ok(tokenService.createToken(userInfo));
//    }
//
//    @DeleteMapping("logout")
//    public R<?> logout(HttpServletRequest request)
//    {
//        String token = SecurityUtils.getToken(request);
//        if (StringUtils.isNotEmpty(token))
//        {
//            String username = JwtUtils.getUserName(token);
//            // 删除用户缓存记录
//            AuthUtil.logoutByToken(token);
//            // 记录用户退出日志
//            sysLoginService.logout(username);
//        }
//        return R.ok();
//    }
//
//    @PostMapping("refresh")
//    public R<?> refresh(HttpServletRequest request)
//    {
//        LoginUser loginUser = tokenService.getLoginUser(request);
//        if (StringUtils.isNotNull(loginUser))
//        {
//            // 刷新令牌有效期
//            tokenService.refreshToken(loginUser);
//            return R.ok();
//        }
//        return R.ok();
//    }
//
//    @PostMapping("register")
//    public R<?> register(@RequestBody RegisterBody registerBody)
//    {
//        // 用户注册
//        sysLoginService.register(registerBody.getUsername(), registerBody.getPassword());
//        return R.ok();
//    }
//
//}
