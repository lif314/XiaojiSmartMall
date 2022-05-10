package com.lif314.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.lif314.gulimall.product.entity.AttrEntity;
import com.lif314.gulimall.product.service.AttrAttrgroupRelationService;
import com.lif314.gulimall.product.service.AttrService;
import com.lif314.gulimall.product.service.CategoryService;
import com.lif314.gulimall.product.vo.AttrGroupRelationVo;
import com.lif314.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.lif314.gulimall.product.entity.AttrGroupEntity;
import com.lif314.gulimall.product.service.AttrGroupService;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.R;


/**
 * 属性分组
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:12:41
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    AttrService attrService;

    @Autowired
    AttrAttrgroupRelationService relationService;


    /**
     * 获取分类下所有分组&关联属性
     *
     * /product/attrgroup/{catelogId}/withattr
     */

    @GetMapping("/{catelogId}/withattr")
    public R getCategoryGroupAttr(@PathVariable("catelogId") Long catelogId){
        // 1 查出分类下的所有属性分组
        // 2 查出属性分组下的所有属性
        List<AttrGroupWithAttrsVo>  vos =  attrGroupService.getAttrGroupWithAttrsByCatelogId(catelogId);
        return R.ok().put("data", vos);
    }


    /**
     * 添加属性与分组关联关系
     * /product/attrgroup/attr/relation
     *
     * [{
     *   "attrGroupId": 0, //分组id
     *   "attrId": 0, //属性id
     * }]
     */
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> vos){
        // 批量保存
        relationService.saveBatchRelation(vos);
        return R.ok();
    }


    /***
     * /product/attrgroup/{attrgroupId}/attr/relation
     *
     * 获取指定分组关联的所有属性
     */

    @GetMapping("/{attrgroupId}/attr/relation")
    public R getAttrRelation(@PathVariable("attrgroupId") Long attrgroupId) {
        List<AttrEntity> attrEntity = attrService.getAttrRelation(attrgroupId);
        return R.ok().put("data", attrEntity);
    }

    /**
     *  获取属性分组没有关联的其他属性
     *  获取属性分组里面还没有关联的本分类里面的其他基本属性，方便添加新的关联
     *  /product/attrgroup/{attrgroupId}/noattr/relation
     */
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R getAttrNoRelation(@RequestParam Map<String, Object> params, @PathVariable("attrgroupId") Long attrgroupId) {
        PageUtils page = attrService.getAttrNoRelationAttr(attrgroupId, params);
        return R.ok().put("page", page);
    }

    /**
     * /product/attrgroup/attr/relation/delete
     * <p>
     * 删除属性与分组的关联关系
     */
    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVo[] relationVos) {
        // 只需要删除关联关系
        attrService.deleteRelation(relationVos);
        return R.ok();
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:attrgroup:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = attrGroupService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 根据类名id查询属性分组
     */
    @RequestMapping("/list/{catelogId}")
    //@RequiresPermissions("product:attrgroup:list")
    public R list(@RequestParam Map<String, Object> params, @PathVariable("catelogId") Long catelogId) {
//        PageUtils page = attrGroupService.queryPage(params);
        // 分页数据查询
        PageUtils page = attrGroupService.queryPage(params, catelogId);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     * <p>
     * 在查询信息时查出菜单路径
     */
    @RequestMapping("/info/{attrGroupId}")
    // @RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        Long catelogId = attrGroup.getCatelogId();
        Long[] path = categoryService.findCatelogPath(catelogId);
        attrGroup.setCatelogPath(path);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
