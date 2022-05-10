package com.lif314.gulimall.authserver.feign;

import com.lif314.common.to.SmsTo;
import com.lif314.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-third-party")
public interface ThirdPartySerrvice {

    @PostMapping("/sms/sendcode")
    R sendCode(@RequestBody SmsTo smsTo);
}
