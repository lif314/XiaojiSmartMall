package com.lif314.gulimall.order.web;


import com.lif314.common.utils.R;
import com.lif314.gulimall.order.service.OrderService;
import com.lif314.gulimall.order.vo.OrderConfirmVo;
import com.lif314.gulimall.order.vo.OrderSubmitVo;
import com.lif314.gulimall.order.vo.SubmitOrderRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@RequestMapping("/order")
@RestController
public class WebOrderController {

    @Autowired
    OrderService orderService;

    /**
     * 处理去结算请求
     */
    @GetMapping("/toTrade")
    public R toTrade() throws ExecutionException, InterruptedException {
        // 获取选中的商品数据
        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        return R.ok().put("data", orderConfirmVo);
    }

    /**
     * 提交订单
     */
    @PostMapping("/submitOrder")
    public R submitOrder(OrderSubmitVo submitVo){

        // 去创建订单，验令牌，验价格，锁库存。。。。

        SubmitOrderRespVo respVo =  orderService.submitOrder(submitVo);

        if(respVo.getCode() == 0){
            // 下单成功来到支付选项
            return R.ok().put("data", respVo);
        }else{
            // 下单失败回到订单确认页重新确认订单信息
            String msg = "下单失败";
            switch (respVo.getCode()){
                case 1: msg = "订单信息过期，请刷新再次提交"; break;
                case 2: msg = "订单商品价格发生变化，请确认后提交"; break;
                case 3: msg = "库存确定失败，商品库存不足"; break;
            }
            // 模拟放在session中，同时放在请求域中
            return R.error().put("data", msg);
        }
    }

}
