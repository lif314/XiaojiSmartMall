package com.lif314.gulimall.order.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemVo {
    private Long skuId; // 商品id

    private Boolean check=true; // 是否被选中

    private String title; // 标题

    private String image;   // 图片

    private List<String> skuAttr; // 选中的商品的属性列表

    private BigDecimal price;  // 价格

    private Integer count;   // 数量

    private BigDecimal totalPrice;  // 数量*价格，

    // TODO 查询有货无货信息
    private boolean hasStock = true;  // 是否有货

    private BigDecimal weight; // 商品重量
}
