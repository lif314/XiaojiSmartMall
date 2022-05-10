package com.lif314.gulimall.product.web;

import com.lif314.gulimall.product.service.SkuInfoService;
import com.lif314.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ItemController {

    @Autowired
    SkuInfoService skuInfoService;


    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable("skuId") Long skuId, Model model){
        SkuItemVo sku = skuInfoService.item(skuId);
        model.addAttribute("item", sku);
        return "item";
    }
}
