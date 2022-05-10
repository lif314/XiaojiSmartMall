package com.lif314.gulimall.member.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberRegisterVo {
    private String userName;

    private String password;

    private String phone;
}
