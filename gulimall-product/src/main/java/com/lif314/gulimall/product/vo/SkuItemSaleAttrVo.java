package com.lif314.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 销售属性的Vo
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuItemSaleAttrVo {
    /**
     * 1.销售属性对应1个attrName
     * 2.销售属性对应n个attrValue
     * 3.n个sku包含当前销售属性（所以前端根据skuId交集区分销售属性的组合【笛卡尔积】）
     */
    private Long attrId;
    private String attrName;
    private List<AttrValueWithSkuIdVo> attrValues;
}
