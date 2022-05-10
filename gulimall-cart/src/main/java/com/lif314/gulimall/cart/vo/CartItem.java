package com.lif314.gulimall.cart.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物项内容
 */
@Data // 不会覆盖自定义get/set
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long skuId; // 商品id

    private Boolean check=true; // 是否被选中

    private String title; // 标题

    private String image;   // 图片

    private List<String> skuAttr; // 选中的商品的属性列表

    private BigDecimal price;  // 价格

    private Integer count;   // 数量

    private BigDecimal totalPrice;  // 数量*价格，


    // 动态计算总价
    public BigDecimal getTotalPrice() {
        return this.price.multiply(new BigDecimal(count.toString()));
    }
}
