package com.lif314.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
@AllArgsConstructor
public class AttrRespVo extends AttrVo {
    private String catelogName;

    private String  groupName;

    // 回显路径
    private Long[] catelogPath;
}
