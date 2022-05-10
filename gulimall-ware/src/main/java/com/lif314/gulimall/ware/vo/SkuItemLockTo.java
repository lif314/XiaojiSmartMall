package com.lif314.gulimall.ware.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每件商品锁几件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuItemLockTo {

    private Long skuId;

    private Integer count;
}
