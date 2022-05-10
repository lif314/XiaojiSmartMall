package com.lif314.gulimall.product.feign;

import com.lif314.common.to.SkuHasStockTo;
import com.lif314.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 仓储服务
 */
@FeignClient("gulimall-ware")
public interface WareFeignService {

    /**
     * 1、R设计的时候可以加上泛型  √
     * 2、直接返回我们想要的结果
     * 3、自己封装解析的结果
     */

    /**
     * 查询是否有库存
     */
    @PostMapping("/ware/waresku/hasstock")
    R getSkuHasStock(@RequestBody List<Long> skuIds);

}
