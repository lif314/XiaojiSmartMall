package com.lif314.common.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


// 远程查询是否有库存
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuHasStockTo {

    private Long skuId;
    private Boolean hasStock;

}
