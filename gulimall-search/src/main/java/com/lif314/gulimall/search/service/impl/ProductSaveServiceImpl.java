package com.lif314.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.lif314.common.constant.EsConstant;
import com.lif314.common.to.es.SkuEsModel;
import com.lif314.gulimall.search.config.GulimallElasticSearchConfig;
import com.lif314.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("ProductSaveService")
public class ProductSaveServiceImpl implements ProductSaveService {


    @Autowired
    RestHighLevelClient restHighLevelClient;

    /**
     * 商品上架
     * @param skuEsModelList 商品ES保存信息
     */
    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModelList) throws IOException {
        // 给ES中建立索引  product 建立映射关系
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel model : skuEsModelList) {
            // 构造保存请求
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(model.getSkuId().toString());
            String s = JSON.toJSONString(model);
            indexRequest.source(s, XContentType.JSON);

            bulkRequest.add(indexRequest);
        }

        // 给索引保存数据 -- 批量保存
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        // TODO 如果批量错误，进行处理
        boolean b = bulk.hasFailures();
        if(b){
            List<String> collect = Arrays.stream(bulk.getItems()).map(BulkItemResponse::getId).collect(Collectors.toList());
            log.error("商品上架错误：{}", collect);
        }

        return b;
    }
}
