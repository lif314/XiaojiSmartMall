package com.lif314.gulimall.product.controller;

import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.R;
import com.lif314.gulimall.product.entity.AttrEntity;
import com.lif314.gulimall.product.entity.ProductAttrValueEntity;
import com.lif314.gulimall.product.entity.SpuInfoEntity;
import com.lif314.gulimall.product.service.AttrService;
import com.lif314.gulimall.product.service.ProductAttrValueService;
import com.lif314.gulimall.product.vo.AttrRespVo;
import com.lif314.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;



/**
 * 商品属性
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:12:41
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    /**
     * 列表
     */
    @RequestMapping("/list")
   // @RequiresPermissions("product:attr:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 获取SPU规格
     * /product/attr/base/listforspu/{spuId}
     */

    @GetMapping("/base/listforspu/{spuId}")
    public R listForSpu(@PathVariable("spuId") Long spuId){
         List<ProductAttrValueEntity> data = productAttrValueService.listForSpu(spuId);

        return R.ok().put("data", data);
    }


    /**
     * 修改商品规格
     *
     * /product/attr/update/{spuId}
     */
    @PostMapping("/update/{spuId}")
    public R updateSpuAttrs(@PathVariable("spuId") Long spuId,
                           @RequestBody List<ProductAttrValueEntity> entities){
        productAttrValueService.updateSpuAttrs(spuId, entities);
        return R.ok();
    }


    /**
     * 一个方法当两个用
     *
     * 获取分类规格参数：/product/attr/base/list/{catelogId}
     *
     * 获取分类的所有销售属性：/product/attr/sale/list/{catelogId}
     *
     * attrType: 1-基本属性   0-销售属性  路径变量
     */

    @RequestMapping("/{attrType}/list/{catelogId}")
    // @RequiresPermissions("product:attr:list")
    public R baseAttrList(@RequestParam Map<String, Object> params,
                          @PathVariable("catelogId") Long catelogId,
                          @PathVariable("attrType") String attrType){
        PageUtils page  = attrService.queryBaseAttrPage(params, catelogId, attrType);
        return R.ok().put("page", page);
    }

    /**
     * 信息
     *
     */
    @RequestMapping("/info/{attrId}")
    //@RequiresPermissions("product:attr:info")
    public R info(@PathVariable("attrId") Long attrId){

        AttrRespVo respVo = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", respVo);
    }

    /**
     * 保存
     * 保存属性并添加到属性组关联表中a
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attr:save")
    public R save(@RequestBody AttrVo attrVo){
		attrService.saveAttr(attrVo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attr:update")
    public R update(@RequestBody AttrVo attrVo){
		attrService.updateAttr(attrVo);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attr:delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
