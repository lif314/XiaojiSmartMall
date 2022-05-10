package com.lif314.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttrRespVo extends AttrVo {
    private String catelogName;

    private String  groupName;

    // 回显路径
    private Long[] catelogPath;
}
