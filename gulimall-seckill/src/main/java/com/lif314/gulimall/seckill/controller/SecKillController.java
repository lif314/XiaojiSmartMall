package com.lif314.gulimall.seckill.controller;

import com.lif314.common.utils.R;
import com.lif314.gulimall.seckill.service.SecKillService;
import com.lif314.gulimall.seckill.to.SecKillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/seckill")
public class SecKillController {

    @Autowired
    SecKillService secKillService;

    /**
     * 查询当前时间可以参与的秒杀商品信息
     */
    @ResponseBody
    @GetMapping("/currentSecKillSkus")
    public R getCurrentSecKillSkus() {
        List<SecKillSkuRedisTo> redisToList = secKillService.getCurrentSecKillSkus();
        return R.ok().put("data", redisToList);
    }

    /**
     * 查询某商品的秒杀信息
     */
    @ResponseBody
    @GetMapping("/seckillInfo/{skuId}")
    public R getSeckillInfoBySkuId(@PathVariable("skuId") Long skuId) {
        SecKillSkuRedisTo data = secKillService.getSkuSeckillInfo(skuId);
        return R.ok().put("data", data);
    }

    /**
     * 处理秒杀请求
     * http://seckill.feihong.com/seckill?killId=" + killId + "&key=" + code + "&num=" + num;
     */
    // TODO 细化功能：1、上架秒杀商品每个商品设置过期时间； 2、秒杀结束后的流程
    @GetMapping("/killSku")
    public R secKill(@RequestParam("killId") String killId,
                          @RequestParam("key") String randomCode,
                          @RequestParam("num") Integer num) {
        // 1、判断是否登录 -- 使用拦截器机制
        String orderSn = secKillService.kill(killId, randomCode, num);
        // 返回订单号
        return R.ok().put("data", orderSn);
    }
}
