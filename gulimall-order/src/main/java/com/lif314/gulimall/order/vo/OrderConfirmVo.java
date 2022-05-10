package com.lif314.gulimall.order.vo;

import com.lif314.gulimall.order.to.MemberAddressTo;
import com.lif314.gulimall.order.to.OrderItemVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单确认页数据模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmVo {

    // 收获地址列表 -- gulimall-member
    List<MemberAddressTo> address;

    // 所有选中的购物项
    List<OrderItemVo> items;


    // 发票信息。。。。


    // 优惠券信息 gulimall-coupon
    Integer interation;


    // 唯一令牌 -- 仿重
    String orderToken;

//    BigDecimal total; // 订单总额
//
//    BigDecimal payPrice; // 应付价格

    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if(items != null && items.size() > 0){
            for (OrderItemVo item : items) {
                BigDecimal itemPrice = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                sum = sum.add(itemPrice);
            }
        }
        return sum;
    }

    public BigDecimal getPayPrice() {
        return getTotal();
    }

    public Integer getCount(){
        // 获取件数
        Integer count = 0;
        if(items != null){
            for (OrderItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }
}
