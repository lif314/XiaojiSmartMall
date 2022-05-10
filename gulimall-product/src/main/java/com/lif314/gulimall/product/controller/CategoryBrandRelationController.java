package com.lif314.gulimall.product.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lif314.gulimall.product.entity.BrandEntity;
import com.lif314.gulimall.product.vo.BrandVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.lif314.gulimall.product.entity.CategoryBrandRelationEntity;
import com.lif314.gulimall.product.service.CategoryBrandRelationService;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.R;



/**
 * 品牌分类关联
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:12:40
 */
@RestController
@RequestMapping("product/categorybrandrelation")
public class CategoryBrandRelationController {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;


    /**
     * 获取分类关联的品牌
     * /product/categorybrandrelation/brands/list
     *
     * "brandId": 0,
     * "brandName": "string",
     */

    @GetMapping("/brands/list")
    public R getRelationBrandsList(@RequestParam(value = "catId", required = true) Long catId){
         List<CategoryBrandRelationEntity> relationEntities = categoryBrandRelationService.getBrandsByCatId(catId);
//        List<BrandEntity> brandEntities =  categoryBrandRelationService.getBrandsByCatId(catId);
        // 该方法可能会其它业务使用，所以在service中返回完整数据，在controoler中封装Vo将其返回
        List<BrandVo> brandVos = relationEntities.stream().map((relationEntity) -> {
            BrandVo brandVo = new BrandVo();
//            BeanUtils.copyProperties(brandEntity, brandVo);
            // 属性名不同，不能对拷
            brandVo.setBrandId(relationEntity.getBrandId());
            brandVo.setBrandName(relationEntity.getBrandName());
            return brandVo;
        }).collect(Collectors.toList());
        return R.ok().put("data", brandVos);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:categorybrandrelation:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = categoryBrandRelationService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 获取品牌关联的分类
     *
     * @param brandId 品牌id
     */
//    @RequestMapping(value = "/catelog/list", method = RequestMethod.GET)
    @GetMapping("/catelog/list")
    //@RequiresPermissions("product:categorybrandrelation:list")
    public R catelogList(@RequestParam Long brandId){
        /**
         * {
         * 	"msg": "success",
         * 	"code": 0,
         * 	"data": [{
         * 		"catelogId": 0,
         * 		"catelogName": "string",
         *        }]
         * }
         */
        // 使用list查询，传入查询条件
        List<CategoryBrandRelationEntity> catelogList = categoryBrandRelationService.list(
                new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId)
        );
        return R.ok().put("data", catelogList);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
   // @RequiresPermissions("product:categorybrandrelation:info")
    public R info(@PathVariable("id") Long id){
		CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return R.ok().put("categoryBrandRelation", categoryBrandRelation);
    }

    /**
     * 新增品牌与分类关联关系
     *
     * 参数：{"brandId":1,"catelogId":2}
     */
    @PostMapping("/save")
    //@RequiresPermissions("product:categorybrandrelation:save")
    public R save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
        categoryBrandRelationService.saveDetail(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
   // @RequiresPermissions("product:categorybrandrelation:update")
    public R update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.updateById(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:categorybrandrelation:delete")
    public R delete(@RequestBody Long[] ids){
		categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
