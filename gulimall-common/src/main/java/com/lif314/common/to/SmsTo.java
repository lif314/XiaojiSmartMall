package com.lif314.common.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码校验
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SmsTo {
    private String phone;
    private String code;
}
