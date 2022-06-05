package com.lif314.gulimall.order.listener;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.lif314.gulimall.order.config.AlipayTemplate;
import com.lif314.gulimall.order.entity.OrderEntity;
import com.lif314.gulimall.order.service.OrderService;
import com.lif314.gulimall.order.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;


/**
 * 处理订单支付成功后的异步通知消息
 */
@RestController
public class OrderPayedListener {

    @Autowired
    OrderService orderService;

    @Autowired
    AlipayTemplate alipayTemplate;

    @PostMapping("/payed/notify") // 需要post请求
    public String handleAliPayed(String vo1, HttpServletRequest request) throws AlipayApiException {
        System.out.println(JSON.toJSONString(vo1));
        System.out.println("支付宝数据到了。。。。");

        PayAsyncVo vo = JSON.parseObject(vo1, new TypeReference<PayAsyncVo>(){});

        // TODO 验签！！！！验证是ali返回的数据
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用
            // valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }

        // 验签
        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayTemplate.getAlipay_public_key(),
                alipayTemplate.getCharset(), alipayTemplate.getSign_type()); //调用SDK验证签名

        // 只要收到了支付宝的异步通知消息，则是订单支付成功，
        // 返回sucess，支付宝再也不继续法通知
        if (signVerified) {
            System.out.println("签名验证成功...");
            //去修改订单状态
            String result = orderService.handlePayResult(vo);
            return result;
        } else {
            System.out.println("签名验证失败...");
            return "error";
        }
    }
}
