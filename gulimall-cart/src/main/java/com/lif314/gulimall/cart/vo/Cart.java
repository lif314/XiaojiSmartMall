package com.lif314.gulimall.cart.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 整个购物车
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    List<CartItem> items;

    private Integer countNum; // 商品的总件数

    private Integer countType; // 有几种商品

    private BigDecimal totalAmount; // 应付总价

    private BigDecimal reduce = new BigDecimal("0"); // 优惠价格


    public Integer getCountNum() {
        int count = 0;
        if(this.items != null && this.items.size() > 0){
            for (CartItem item : this.items) {
                count += item.getCount();
            }
        }
        return count;
    }

    public Integer getCountType() {
        if(this.items != null){
            return this.items.size();
        }
        return 0;
    }

    public BigDecimal getTotalAmount() {
        // 计算商品总价
        BigDecimal amount = new BigDecimal("0");
        // 计算购物项总价
        if(this.items != null && this.items.size() > 0){
            for (CartItem item : this.items) {
                if(item.getCheck()){
                    BigDecimal totalPrice = item.getTotalPrice();
                    amount = amount.add(totalPrice);
                }
            }
        }
        // 减去优惠价格
        BigDecimal subtract = amount.subtract(getReduce());
        return subtract;
    }

    public BigDecimal getReduce() {
        return reduce;
    }
}
