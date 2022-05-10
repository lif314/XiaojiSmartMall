package com.lif314.gulimall.product.feign;

import com.lif314.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-seckill")
public interface SeckillFeignService {
    /**
     * 查询某商品的秒杀信息
     */
    @GetMapping("/seckill/seckillInfo/{skuId}")
    R getSeckillInfoBySkuId(@PathVariable("skuId") Long skuId);
}
