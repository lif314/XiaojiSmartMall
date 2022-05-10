package com.lif314.gulimall.member.feign;

import com.lif314.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;


@FeignClient("gulimall-coupon") // 注册中心的服务名
public interface CouponFeignService {
    // 路径需要全
    @RequestMapping("coupon/coupon/member/list")
    public R membercoupons();
    }
