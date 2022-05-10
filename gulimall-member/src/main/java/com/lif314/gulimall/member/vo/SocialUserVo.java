package com.lif314.gulimall.member.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialUserVo {
    private Long socialUid;// 用户id
    private String socialType; // 社交登录类型
}
