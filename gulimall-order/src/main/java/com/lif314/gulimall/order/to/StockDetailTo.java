package com.lif314.gulimall.order.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作单详情
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDetailTo {
    private Long id;
    /**
     * sku_id
     */
    private Long skuId;
    /**
     * sku_name
     */
    private String skuName;
    /**
     * 购买个数
     */
    private Integer skuNum;
    /**
     * 工作单id
     */
    private Long taskId;
    /**
     * 仓库id
     */
    private Long wareId;
    /**
     * 1-已锁定  2-已解锁  3-扣减
     */
    private Integer lockStatus;
}
