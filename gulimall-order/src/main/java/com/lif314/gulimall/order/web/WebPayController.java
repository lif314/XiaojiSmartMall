package com.lif314.gulimall.order.web;


import com.alipay.api.AlipayApiException;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.R;
import com.lif314.gulimall.order.config.AlipayTemplate;
import com.lif314.gulimall.order.service.OrderService;
import com.lif314.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class WebPayController {


    @Autowired
    AlipayTemplate alipayTemplate;

    @Autowired
    OrderService orderService;

    /**
     * 支付订单
     * 1、将支付页面直接交给浏览器进行渲染
     * 2、支付成功后跳转到用户页面
     * th:href= "'http://order.feihong.com/PayOrder?orderSn='+${submitOrderResp.order.orderSn}"
     */
    @GetMapping(value = "/payOrder")  // 告诉产生html数据，而不是json数据
    public R payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
//        System.out.println("Pay Order, OrderSn: " + orderSn);
        // 获取订单的支付信息
        PayVo payVo = orderService.getOrderByOrderSn(orderSn);
        // 调用alipay -- 响应数据为HTML表单
        // 将此页面直接交给浏览器进行渲染
        String pay = alipayTemplate.pay(payVo);
        orderService.updateOrderStatus(orderSn);
        // 这是一个页面模板，直接让浏览器渲染
//        System.out.println(pay);
        return R.ok().put("data", pay);
    }


    /**
     * 用户订单列表 -- 分页查询
     *
     * 1、支付成功的回调页
     *
     * memberOrder.html
     */
    @GetMapping("/memberOrder")
    public R memberOrderPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum){
        // 支付成功，改变订单状态
        // 使用支付宝异步通知 notify_url -- 必须要要支付宝可以访问的地址
        Map<String, Object> map = new HashMap<>();
        map.put("page", pageNum.toString());
        PageUtils page = orderService.queryPageWithItem(map);
//        model.addAttribute("orders", page);
//        System.out.println(JSON.toJSONString(page));
        // TODO 支付宝验签后更改订单的状态
        return R.ok().put("data", page);
    }
}
