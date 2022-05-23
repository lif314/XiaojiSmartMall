package com.lif314.gulimall.order.feign;

import com.lif314.gulimall.order.to.OrderItemVo;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;


@FeignClient("gulimall-cart")
@Headers({"TOKEN:${token}"})
public interface CartFeignService {
    /**
     * 获取当前用户选中的购物项
     */
    @ResponseBody
    @GetMapping("/cart/currentUserCartItems")
    List<OrderItemVo> getCurrentUserCartItems();
}
