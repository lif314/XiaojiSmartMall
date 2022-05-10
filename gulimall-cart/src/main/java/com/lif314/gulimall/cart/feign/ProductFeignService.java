package com.lif314.gulimall.cart.feign;


import com.lif314.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    // 获取商品详细信息 return R.ok().put("skuInfo", skuInfo);
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R getSkuInfo(@PathVariable("skuId") Long skuId);


    /**
     * 购物车显示：查询商品的所有销售属性的值
     */
    @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
    List<String> getSkuSaleAttrValues(@PathVariable("skuId") Long skuId);

    /**
     * 获取购物项中商品的最新价格
     */
    @GetMapping("/product/skuinfo/getNewPrices")
    R getCartItemNewPrices(@RequestParam List<Long> ids);

}
