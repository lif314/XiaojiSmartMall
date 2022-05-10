package com.lif314.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttrGroupRelationVo {
    // [{"attrId":1,"attrGroupId":2}]
    private Long attrId;
    private Long attrGroupId;
}
