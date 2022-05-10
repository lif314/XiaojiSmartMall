package com.lif314.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lif314.common.to.mq.SeckillOrderTo;
import com.lif314.common.utils.PageUtils;
import com.lif314.gulimall.order.entity.OrderEntity;
import com.lif314.gulimall.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:03:07
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderRespVo submitOrder(OrderSubmitVo submitVo);

    OrderEntity getOrderStatusByOrderSn(String orderSn);

    void closeOrder(OrderEntity entity);

    PayVo getOrderByOrderSn(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    String handlePayResult(PayAsyncVo vo);

    void updateOrderStatus(String orderSn);

    void cereateSeckillOrder(SeckillOrderTo seckillOrder);
}

