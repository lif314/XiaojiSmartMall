package com.lif314.gulimall.ware.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockStockResult {

    private Long skuId;
    private Integer num; // 锁了几件
    private boolean locked; // 是否锁定成功
}
