package com.lif314.gulimall.order.vo;

import com.lif314.gulimall.order.entity.OrderEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下单相应数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitOrderRespVo {

    private OrderEntity order;

    private Integer code;  // 状态码
}
