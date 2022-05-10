package com.lif314.gulimall.ware.feign;

import com.lif314.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-order")
public interface OrderFeignService {

    /**
     * 根据订单号查询订单状态
     */
    @GetMapping("/order/order/status/{orderSn}")
    R getOrderStatusByOrderSn(@PathVariable("orderSn") String orderSn);
}
