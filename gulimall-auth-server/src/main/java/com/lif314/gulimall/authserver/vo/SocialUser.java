package com.lif314.gulimall.authserver.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * gitee获取token响应数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialUser {
        /**
         * 返回用户数据，大都是没有用的
         * {
         *     "id": 7812289,
         *     "login": "lilinfei314",
         *     "name": "lilinfei314",
         *     "email": null
         *     ...
         * }
         */
        private Long socialUid;// 用户id
        private String socialType; // 社交登录类型
}
