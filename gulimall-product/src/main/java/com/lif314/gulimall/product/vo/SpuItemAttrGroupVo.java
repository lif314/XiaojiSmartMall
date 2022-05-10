package com.lif314.gulimall.product.vo;

import com.lif314.gulimall.product.saveVo.Attr;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class SpuItemAttrGroupVo {
    // 分组信息
    private String groupName;
    private List<Attr> attrs;
}
