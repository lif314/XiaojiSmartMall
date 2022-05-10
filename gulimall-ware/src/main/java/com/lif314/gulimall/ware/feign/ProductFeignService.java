package com.lif314.gulimall.ware.feign;


import com.lif314.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
     * product/skuinfo/info/{skuId}  给 gulimall-product 发送请求
     *
     * api/product/skuinfo/info/{skuId} 给 gulimall-gateway发送请求
     *
     * 远程获取sku信息
     */
    @RequestMapping("product/skuinfo/info/{skuId}")
    R info(@PathVariable("skuId") Long skuId);

}
