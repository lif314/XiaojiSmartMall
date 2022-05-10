package com.lif314.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.lif314.common.constant.EsConstant;
import com.lif314.common.to.es.SkuEsModel;
import com.lif314.gulimall.search.config.GulimallElasticSearchConfig;
import com.lif314.gulimall.search.service.SearchService;
import com.lif314.gulimall.search.vo.SearchParamVo;
import com.lif314.gulimall.search.vo.SearchResultVo;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.util.UriEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("SearchService")
public class SearchServiceImpl implements SearchService {

    @Autowired
    RestHighLevelClient client;

    // 在ES中进行检索
    @Override
    public SearchResultVo search(SearchParamVo searchParam) {
        // 1、动态构建出查询需要的DSL语句

        // 1、准备检索请求
        SearchRequest searchRequest = buildSearchRequest(searchParam);

        SearchResultVo result = null;
        try {
            // 2、执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
            // 3、分析检索响应
            result = buildSearchResult(response, searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 构建检索请求
     * 模糊匹配，过滤(按照属性、分类、品牌、价格区间、库存); 排序、分页；高亮，聚合分析
     */
    private SearchRequest buildSearchRequest(SearchParamVo param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        /**
         * 模糊匹配，过滤(按照属性、分类、品牌、价格区间、库存)
         */
        // 构建bool query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 1.1 bool-must  模糊匹配 skuTitle
        if (!StringUtils.isEmpty(param.getKeyword())) {
            // 搜索查询
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }

        // 1.2 bool-filter 按照属性、分类、品牌、价格区间、库存
        if (param.getCatalog3Id() != null) {
            // 三级分类
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        // 1.2 bool-filter 按照品牌id查询
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            // 品牌Id查询
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // TODO 1.2 bool-filter 按照属性查询
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attrStr : param.getAttrs()) {
                // 每一组属性查询构建一个nestedQuery
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                // attrs=1_5寸:6存&attrs=2_166:87
                String[] s = attrStr.split("_");
                String attrId = s[0]; // 检索属性id
                String[] attrValues = s[1].split(":");  // 这个属性的检索值
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                // 每一个属性必须生成一个nested查询 -- 不参与评分
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        // 1.2 bool-filter 按照是否有库存进行查询 0无库存  1有库存 默认为1
        if(param.getHasStock() != null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }
        // 1.2 bool-filter 按照价格区间 1_500/_500/500_
        if (StringUtils.isNotEmpty(param.getSkuPrice())) {
            // 1_500/_500/500_   组装range  gte/lte
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            // 分割参数
            String[] prices = param.getSkuPrice().split("_");
            if (prices.length == 2) {
                // 区间查询
                rangeQuery.gte(prices[0]).lte(prices[1]);
            } else if (prices.length == 1) {
                if (param.getSkuPrice().startsWith("_")) {
                    // 小于
                    rangeQuery.lte(prices[0]);
                } else {
                    // 大于
                    rangeQuery.gte(prices[0]);
                }
            }
            boolQuery.filter(rangeQuery);
        }

        // 模糊匹配加入查询条件
        sourceBuilder.query(boolQuery);

        /**
         * 排序、分页；高亮
         */
        // 2.1 排序
        if(StringUtils.isNotEmpty(param.getSort())){
            /**
             * 排序：sort=saleCount_asc  sort=hotScore_asc  sort=skuPrice_asc
             *      sort=saleCount_desc  sort=hotScore_desc sort=skuPrice_desc
             */
            String sort = param.getSort();
            String[] s = sort.split("_");
            // 顺序
            SortOrder sortOrder = s[1].equalsIgnoreCase("asc")?SortOrder.ASC:SortOrder.DESC;
            sourceBuilder.sort(s[0], sortOrder);
        }
        // 2.2 分页
        // pageNum:  1 from: 0 size:5 [0,1,2,3,4]
        // from = (pageNum-1)*size
        sourceBuilder.from((param.getPageNum()-1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 2.3 高亮
        if(StringUtils.isNotEmpty(param.getKeyword())){
            // 只有模糊查询才有高亮
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }

        /**
         * 聚合分析
         */
        // 3.1 品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        // 品牌聚合子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);

        // 3.2 分类聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);

        // 3.3 属性聚合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(50);
        attr_agg.subAggregation(attr_id_agg);
        // 聚合分析出当前attr_id对应的属性名字和对应的所有可能属性值attrValues
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        sourceBuilder.aggregation(attr_agg);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return  searchRequest;
    }

    /**
     * 分析检索结果--封装数据
     */
    private SearchResultVo buildSearchResult(SearchResponse response, SearchParamVo param) {
        SearchResultVo result = new SearchResultVo();

        // 1. 返回查询到的所有商品
        // 总记录数
        SearchHits hits = response.getHits();
        List<SkuEsModel> skuEsModelList = new ArrayList<>();
        if(hits.getHits() != null && hits.getHits().length > 0){
            for (SearchHit hit : hits.getHits()) {
                // 获取每一条记录
                String sourceAsString = hit.getSourceAsString();   // _source
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);

                // 判断高亮信息
                if(StringUtils.isNotEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String newTitle = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(newTitle);
                }
                skuEsModelList.add(skuEsModel);
            }
        }
        result.setProducts(skuEsModelList);


        // 所有聚合信息
        Aggregations aggregations = response.getAggregations();


        // 2. 当前商品涉及的分类信息
        ParsedLongTerms catalog_agg = aggregations.get("catalog_agg");
        List<SearchResultVo.CatalogVo> catalogVos  =  new ArrayList<>();
        String catalogName = null;// 面包屑map数据源【分类】
        for (Terms.Bucket bucket : catalog_agg.getBuckets()) {
            // 获取分类id和名字
            SearchResultVo.CatalogVo catalogVo = new SearchResultVo.CatalogVo();
            catalogVo.setCatalogId(bucket.getKeyAsNumber().longValue());
            // 获取分类名字
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalog_name);
            catalogVos.add(catalogVo);
            // 构建面包屑数据源
            if (catalogVo.getCatalogId().equals(param.getCatalog3Id())) {
                catalogName = catalogVo.getCatalogName();
            }
        }
        result.setCatalogs(catalogVos);

        // 3. 当前所有商品涉及到的品牌信息
        ParsedLongTerms brand_agg = aggregations.get("brand_agg");
        List<SearchResultVo.BrandVo> brandVos = new ArrayList<>();
        Map<Long, String> brandMap = new HashMap<>();// 面包屑map数据源【品牌】
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            // 获取品牌的id 名字  图片
            SearchResultVo.BrandVo brandVo = new SearchResultVo.BrandVo();
            // id
            long brandId = bucket.getKeyAsNumber().longValue();
            // 图片
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            // 名字
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();

            brandVo.setBrandId(brandId);
            brandVo.setBrandImg(brandImg);
            brandVo.setBrandName(brandName);
            brandVos.add(brandVo);

            // 构建面包屑数据源
            if (!CollectionUtils.isEmpty(param.getBrandId()) ) {
                brandMap.put(brandVo.getBrandId(), brandVo.getBrandName());
            }
        }
        result.setBrands(brandVos);



        Map<Long, String> attrMap = new HashMap<>();// 面包屑map数据源【属性名】
        // 4. 当前商品涉及的所有属性信息
        ParsedNested attr_agg = aggregations.get("attr_agg");
        List<SearchResultVo.AttrVo> attrVos = new ArrayList<>();
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResultVo.AttrVo attrVo =  new SearchResultVo.AttrVo();
            // 属性id
            long attr_id = bucket.getKeyAsNumber().longValue();
            // 属性名字
            String attr_name = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            // 属性所有值
            List<String> attr_values = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg")).getBuckets().stream().map((item) -> {
                return item.getKeyAsString();
            }).collect(Collectors.toList());

            // 设置
            attrVo.setAttrId(attr_id);
            attrVo.setAttrName(attr_name);
            attrVo.setAttrValue(attr_values);
            // 加入集合
            attrVos.add(attrVo);

            // 构建面包屑数据源
            if (!CollectionUtils.isEmpty(param.getAttrs()) && !attrMap.containsKey(attrVo.getAttrId())) {
                attrMap.put(attrVo.getAttrId(), attrVo.getAttrName());
            }
        }
        result.setAttrs(attrVos);

        // ===========以上是聚合信息获取========
        // 5. 分页信息
        // 5.1 页码
        result.setPageNum(param.getPageNum());
        // 5.2 总记录数
        long totalHits = hits.getTotalHits().value;
        result.setTotal(totalHits);
        // 5.3 总页码
        int totalPages = (totalHits%EsConstant.PRODUCT_PAGESIZE == 0? (int) totalHits/EsConstant.PRODUCT_PAGESIZE : ((int)totalHits/EsConstant.PRODUCT_PAGESIZE + 1));
        result.setTotalPages(totalPages);
        // 5.4 导航页码
        List<Integer> pageNavs = new ArrayList<>();
        for(int i = 1; i <= totalPages; i++ )
        {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);


        //  TODO 6. 构建面包屑导航数据_属性
        List<SearchResultVo.NavVo> navs = new ArrayList<>();
        if (!CollectionUtils.isEmpty(param.getAttrs())) {
            // 属性非空才需要面包屑功能
            navs = param.getAttrs().stream().map(attr -> {
                // attr：15_海思 分析ttrs传过来的参数值
                SearchResultVo.NavVo nav = new SearchResultVo.NavVo();
                String[] arr = attr.split("_"); // id_值
                // 封装筛选属性ID集合【给前端判断哪些属性是筛选条件，从而隐藏显示属性栏，显示在面包屑中】
                result.getAttrIds().add(Long.parseLong(arr[0]));
                // 面包屑名字：属性名
                nav.setNavName(attrMap.get(Long.parseLong(arr[0])));
                // 面包屑值：属性值
                nav.setNavValue(arr[1]);
                // 设置跳转地址（将属性条件置空）【当取消面包屑上的条件时，跳转地址】
                String replace = replaceQueryString(param, "attrs", attr);
                nav.setLink("http://search.feihong.com/search.html?" + replace);// 每一个属性都有自己对应的回退地址

                return nav;
            }).collect(Collectors.toList());
        }

        // 7.构建面包屑导航数据_品牌
        if (!CollectionUtils.isEmpty(param.getBrandId())) {
            // 多个品牌ID封装成一级面包屑，所以这里只需要一个NavVo
            SearchResultVo.NavVo nav = new SearchResultVo.NavVo();
            // 面包屑名称直接使用品牌
            nav.setNavName("品牌");
            StringBuffer buffer = new StringBuffer();
            String replace = "";
            for (Long brandId : param.getBrandId()) {
                // 多个brandId筛选条件汇总为一级面包屑，所以navValue拼接所有品牌名
                buffer.append(brandMap.get(brandId)).append(";");
                // 因为多个brandId汇总为一级面包屑，所以每一个brandId筛选条件都要删除
                replace = replaceQueryString(param, "brandId", brandId.toString());
            }
            nav.setNavValue(buffer.toString());// 品牌拼接值
            nav.setLink("http://search.feihong.com/search.html?" + replace);// 回退品牌面包屑等于删除所有品牌条件

            navs.add(nav);
        }

        // TODO 分类不需要导航取消
        // 构建面包屑导航数据_分类
//        if (param.getCatalog3Id() != null) {
//            SearchResult.NavVo nav = new SearchResult.NavVo();
//            nav.setNavName("分类");
//            nav.setNavValue(catalogName);// 分类名
//            StringBuffer buffer = new StringBuffer();
////            String replace = replaceQueryString(param, "catalog3Id", param.getCatalog3Id().toString());
////            nav.setLink("http://search.gulimall.com/list.html?" + replace);
//
//            navs.add(nav);
//        }


        // 设置面包屑导航
        result.setNavs(navs);

        return result;
    }

    private String replaceQueryString(SearchParamVo param, String key, String value) {
        // 解决编码问题，前端参数使用UTF-8编码了
        String encode = null;
        encode = UriEncoder.encode(value);
//                try {
//                    encode = URLEncoder.encode(attr, "UTF-8");// java将空格转义成了+号
//                    encode = encode.replace("+", "%20");// 浏览器将空格转义成了%20，差异化处理，否则_queryString与encode匹配失败
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
        // TODO BUG，第一个参数不带&
        // 替换掉当前查询条件，剩下的查询条件即是回退地址
        String replace = param.get_queryString().replace("&" + key + "=" + encode, "");
        return replace;
    }


}
