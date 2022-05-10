package com.lif314.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 销售属性值关联的skuid
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttrValueWithSkuIdVo {
    private String attrValue;
    private String skuIds; // 关联的skuId集合，以，隔开
}
