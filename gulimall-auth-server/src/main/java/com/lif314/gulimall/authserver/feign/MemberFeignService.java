package com.lif314.gulimall.authserver.feign;

import com.lif314.common.utils.R;
import com.lif314.gulimall.authserver.vo.SocialUser;
import com.lif314.gulimall.authserver.vo.UserLoginVo;
import com.lif314.gulimall.authserver.vo.UserRegisterVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {
    /**
     * 注册用户
     */
    @PostMapping("/member/member/register")
    R register(@RequestBody UserRegisterVo vo);

    /**
     * 用户登录
     */
    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    /*
     * 社交登录
     */
    @PostMapping("/member/member/oauth2/login")
    R oauthlogin(@RequestBody SocialUser socialUser);

}
