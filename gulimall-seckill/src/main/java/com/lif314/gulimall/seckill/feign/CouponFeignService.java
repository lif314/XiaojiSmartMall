package com.lif314.gulimall.seckill.feign;


import com.lif314.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    /**
     * 获取三天内的秒杀信息
     */
    @GetMapping("/coupon/seckillsession/latest3daysesseion")
    R getLatest3DaysSession();
}
