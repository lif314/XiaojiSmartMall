package com.lif314.gulimall.thirdparty.controller;

import com.lif314.common.to.SmsTo;
import com.lif314.common.utils.R;
import com.lif314.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sms")
public class SmsSendController {

    @Autowired
    SmsComponent smsComponent;

    // 提供给其它服务调用
    @PostMapping("/sendcode")
    public R sendCode(@RequestBody SmsTo smsTo){
        smsComponent.sendSmsCode(smsTo.getPhone(), smsTo.getCode());
        return R.ok();
    }
}
