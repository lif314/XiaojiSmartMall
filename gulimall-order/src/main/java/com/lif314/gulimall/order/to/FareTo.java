package com.lif314.gulimall.order.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FareTo {
    private MemberAddressTo address;

    private BigDecimal fare;
}
