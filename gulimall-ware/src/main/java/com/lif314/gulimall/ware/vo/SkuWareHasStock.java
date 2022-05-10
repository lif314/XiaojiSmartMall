package com.lif314.gulimall.ware.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 获取商品在哪些仓库中
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuWareHasStock {
    private Long skuId;

    private List<Long> wareIds;

    private Integer num; // 锁几件

}
