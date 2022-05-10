package com.lif314.gulimall.order.feign;

import com.lif314.gulimall.order.to.MemberAddressTo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    // 用户收获地址列表
    @GetMapping("/member/memberreceiveaddress/{memberId}/addresses")
    List<MemberAddressTo> getMemberAddress(@PathVariable("memberId") Long memberId);

}
