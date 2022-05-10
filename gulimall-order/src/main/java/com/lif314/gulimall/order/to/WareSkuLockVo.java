package com.lif314.gulimall.order.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单保存成功后的锁定库存
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WareSkuLockVo {
    private String orderSn; // 订单号

    private List<SkuItemLockTo> locks;
}
