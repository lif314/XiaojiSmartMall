package com.lif314.gulimall.product.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.lif314.gulimall.product.vo.CartItemPriceMapVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.lif314.gulimall.product.entity.SkuInfoEntity;
import com.lif314.gulimall.product.service.SkuInfoService;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.R;



/**
 * sku信息
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:12:40
 */
@RestController
@RequestMapping("product/skuinfo")
public class SkuInfoController {
    @Autowired
    private SkuInfoService skuInfoService;


    /**
     * 获取购物项中商品的最新价格
     */
    @GetMapping("/getNewPrices")
    public R getCartItemNewPrices(@RequestParam List<Long> ids){
        CartItemPriceMapVo cartItemNewPrices = skuInfoService.getCartItemNewPrices(ids);
        return R.ok().put("data", cartItemNewPrices);
    }

    /**
     * sku检索
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:skuinfo:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = skuInfoService.queryPageByConditions(params);

        return R.ok().put("page", page);
    }


    /**
     * 查询商品详情
     */
    @RequestMapping("/info/{skuId}")
   // @RequiresPermissions("product:skuinfo:info")
    public R info(@PathVariable("skuId") Long skuId){
		SkuInfoEntity skuInfo = skuInfoService.getById(skuId);

        return R.ok().put("skuInfo", skuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:skuinfo:save")
    public R save(@RequestBody SkuInfoEntity skuInfo){
		skuInfoService.save(skuInfo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
   // @RequiresPermissions("product:skuinfo:update")
    public R update(@RequestBody SkuInfoEntity skuInfo){
		skuInfoService.updateById(skuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:skuinfo:delete")
    public R delete(@RequestBody Long[] skuIds){
		skuInfoService.removeByIds(Arrays.asList(skuIds));

        return R.ok();
    }

}
