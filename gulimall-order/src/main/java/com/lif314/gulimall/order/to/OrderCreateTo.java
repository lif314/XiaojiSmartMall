package com.lif314.gulimall.order.to;

import com.lif314.gulimall.order.entity.OrderEntity;
import com.lif314.gulimall.order.entity.OrderItemEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateTo {

    private OrderEntity order;

    private List<OrderItemEntity> orderItems;

    private BigDecimal payPrice; // 计算订单应付价格

    private BigDecimal fare; // 运费
}
