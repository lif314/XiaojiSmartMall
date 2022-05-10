package com.lif314.gulimall.ware.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 获取收货人信息
 *
 * 运费
 * 收货人地址信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FareVo {

    private MemberAddressVo address;

    private BigDecimal fare;
}
