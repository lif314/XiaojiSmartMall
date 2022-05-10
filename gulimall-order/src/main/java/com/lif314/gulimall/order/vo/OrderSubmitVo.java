package com.lif314.gulimall.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 封装订单提交的数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitVo {

    private Long addrId;  // 收获地址id

    private Integer payType; // 支付方式

    // 无需提交需要购买的商品，去购物车中再获取一遍

    // TODO 优惠信息、发票信息

    private String orderToken;  // 防重令牌

    private BigDecimal payPrice;  // 应付价格  验价

    private String note;   // 订单备注

    // 用户信息，直接去session中取出登录的用户
}
