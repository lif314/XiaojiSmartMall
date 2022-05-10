package com.lif314.gulimall.cart.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未登陆前的
 * 临时用户
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoTo {

    private Long userId;

    private String userKey;

    // 是否有临时用户
    private boolean tempUser = false;

}
