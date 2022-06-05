package com.lif314.gulimall.member;

import com.lif314.gulimall.member.entity.MemberEntity;
import com.lif314.gulimall.member.service.MemberService;
import com.lif314.gulimall.member.vo.MemberLoginVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallMemberApplicationTests {
    @Autowired
    MemberService memberService;

    // 用户登录测试
    @Test
    void userLoginTest(){
        MemberLoginVo vo = new MemberLoginVo("Admin", "123456");
        try{
            MemberEntity login = memberService.login(vo);
            System.out.println("登录账户信息：" + login);
        }catch (Exception e){
            // 打印错误信息
            System.out.println("错误信息：" + e.getMessage());
        }
    }
}
