package com.lif314.gulimall.ware.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.lif314.gulimall.ware.vo.MergeVo;
import com.lif314.gulimall.ware.vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.lif314.gulimall.ware.entity.PurchaseEntity;
import com.lif314.gulimall.ware.service.PurchaseService;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.R;



/**
 * 采购信息
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:20:38
 */
@RestController
@RequestMapping("ware/purchase")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;

    /**
     * 查询出【新建的】和【已分配】的采购单 -- 未领取的采购单
     * /ware/purchase/unreceive/list
     */
    @GetMapping("/unreceive/list")
    public R unreceiveList(@RequestParam Map<String, Object> params){
        PageUtils page = purchaseService.queryPageUnreceiveList(params);
        return R.ok().put("page", page);
    }

    /**
     * 合并采购需求
     *
     * /ware/purchase/merge
     */
    @PostMapping("/merge")
    public R mergeProcurementRequirements(@RequestBody MergeVo mergeVo){
        purchaseService.mergeProcurements(mergeVo);
        return R.ok();
    }

    /**
     * /ware/purchase/received
     *
     * 领取采购单
     *
     * 请求参数：[1,2,3,4]   //采购单id
     */
    @PostMapping("/received")
    public R received(@RequestBody List<Long> ids){
        purchaseService.received(ids);
        return R.ok();
    }

    /**
     * 完成采购
     *
     * /ware/purchase/done
     */
    @PostMapping("/done")
    public R done(@RequestBody PurchaseDoneVo purchaseDoneVo){
        purchaseService.purchaseDone(purchaseDoneVo);
        return R.ok();
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
//    @RequiresPermissions("ware:purchase:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = purchaseService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
//    @RequiresPermissions("ware:purchase:info")
    public R info(@PathVariable("id") Long id){
		PurchaseEntity purchase = purchaseService.getById(id);

        return R.ok().put("purchase", purchase);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
//    @RequiresPermissions("ware:purchase:save")
    public R save(@RequestBody PurchaseEntity purchase){
        purchase.setCreateTime(new Date());
        purchase.setUpdateTime(new Date());
		purchaseService.save(purchase);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
//    @RequiresPermissions("ware:purchase:update")
    public R update(@RequestBody PurchaseEntity purchase){
		purchaseService.updateById(purchase);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
//    @RequiresPermissions("ware:purchase:delete")
    public R delete(@RequestBody Long[] ids){
		purchaseService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
