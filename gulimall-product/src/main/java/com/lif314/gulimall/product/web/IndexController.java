package com.lif314.gulimall.product.web;

import com.lif314.common.utils.R;
import com.lif314.gulimall.product.service.CategoryService;
import com.lif314.gulimall.product.service.SkuInfoService;
import com.lif314.gulimall.product.vo.Catelog2Vo;
import com.lif314.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    SkuInfoService skuInfoService;

    @GetMapping("/getBaseCategoryList")
    public R getCatelogJson(){
        Map<String, List<Catelog2Vo>> map =  categoryService.getCatelogJson();
        return R.ok().put("data", map);
    }



    /**
     * 查询商品详情
     */
    @GetMapping("/item/{skuId}")
    public R skuItem(@PathVariable("skuId") Long skuId){
        SkuItemVo sku = skuInfoService.item(skuId);
        return R.ok().put("data", sku);
    }

}
