package com.lif314.gulimall.order.controller;

import java.util.Arrays;
import java.util.Map;

////import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.lif314.gulimall.order.entity.OrderEntity;
import com.lif314.gulimall.order.service.OrderService;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.R;



/**
 * 订单
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:03:07
 */
@RestController
@RequestMapping("order/order")
public class OrderController {
    @Autowired
    private OrderService orderService;


    /**
     * 根据订单号查询订单状态
     */
    @GetMapping("/status/{orderSn}")
    public R getOrderStatusByOrderSn(@PathVariable("orderSn") String orderSn){
        OrderEntity orderEntity = orderService.getOrderStatusByOrderSn(orderSn);
        if(orderEntity != null){
            return R.ok().put("data", orderEntity.getStatus()); // 只返回订单的状态即可
        }else{
            return R.error().put("data", "该订单不存在！");
        }

    }

    /**
     * 列表
     */
    @RequestMapping("/list")
//    //@RequiresPermissions("order:order:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = orderService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
//    //@RequiresPermissions("order:order:info")
    public R info(@PathVariable("id") Long id){
		OrderEntity order = orderService.getById(id);

        return R.ok().put("order", order);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
//    //@RequiresPermissions("order:order:save")
    public R save(@RequestBody OrderEntity order){
		orderService.save(order);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
//    //@RequiresPermissions("order:order:update")
    public R update(@RequestBody OrderEntity order){
		orderService.updateById(order);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
//    //@RequiresPermissions("order:order:delete")
    public R delete(@RequestBody Long[] ids){
		orderService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
