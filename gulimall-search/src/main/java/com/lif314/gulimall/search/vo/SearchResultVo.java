package com.lif314.gulimall.search.vo;

import com.lif314.common.to.es.SkuEsModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询返回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultVo {

    private List<SkuEsModel> products;// es检索到的所有商品信息

    /**
     * 分页信息
     */
    private Integer pageNum;// 当前页码
    private Long total;// 总记录数
    private Integer totalPages;// 总页码
    private List<Integer> pageNavs;// 导航页码[1、2、3、4、5]

    /**
     * 搜索小米 会返回涉及的品牌信息/分类信息
     */
    private List<BrandVo> brands;// 当前查询到的结果所有涉及到的品牌
    // 提交分类的id
    private List<CatalogVo> catalogs;// 当前查询到的结果所有涉及到的分类

    /**
     * 属性信息
     * attrs=1_anzhuo&attrs=5_其他:1080P
     */
    private List<AttrVo> attrs = new ArrayList<>();// 当前查询到的结果所有涉及到的属性【符合检索条件的，可检索的属性】


    // ============================以上是要返回的数据====================================

    // 封装筛选条件中的属性id集合【用于面包屑，选择属性后出现在面包屑中，下面的属性栏则隐藏】
    // 该字段是提供前端用的
    private List<Long> attrIds = new ArrayList<>();

    // 面包屑导航数据
    private List<NavVo> navs = new ArrayList<>();
    /**
     * 面包屑导航VO
     */
    @Data
    public static class NavVo {
        private String navName;// 属性名
        private String navValue;// 属性值
        private String link;// 回退地址（删除该面包屑筛选条件回退地址）
    }

    /**
     * 相关的品牌信息
     */
    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    /**
     * 相关的分类的信息
     */
    @Data
    public static class CatalogVo {
        private Long catalogId;
        private String catalogName;
    }

    @Data
    public static class AttrVo {
        private Long attrId;    // 允许检索的 属性Id
        private String attrName;// 允许检索的 属性名
        private List<String> attrValue;// 属性值【多个】
    }

}
