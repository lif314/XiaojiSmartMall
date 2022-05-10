package com.lif314.gulimall.product.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 二级分类Vo
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Catelog2Vo {

    private String catalog1Id; // 一级分类的Id
    private List<Category3Vo> catalog3List; // 三级分类
    private String id;
    private String name;


    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Category3Vo{
        private String catalog2Id;  // 二级分类Id
        private String id;
        private String name;
    }
}
