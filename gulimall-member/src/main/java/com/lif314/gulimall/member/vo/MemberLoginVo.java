package com.lif314.gulimall.member.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberLoginVo {
    private String loginacct; // 登录账号
    private String password;
}
