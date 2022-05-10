package com.lif314.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lif314.common.to.SkuHasStockTo;
import com.lif314.common.to.mq.StockLockedTo;
import com.lif314.common.utils.PageUtils;
import com.lif314.gulimall.ware.entity.WareSkuEntity;
import com.lif314.gulimall.ware.vo.LockStockResult;
import com.lif314.gulimall.ware.vo.OrderEntityVo;
import com.lif314.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:20:38
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockTo> getSkuHasStock(List<Long> skuIds);

    //    List<LockStockResult> orderLockStock(WareSkuLockVo vo);
    Boolean orderLockStock(WareSkuLockVo vo);

    void handleUnLockStockWare(StockLockedTo to);

    void handleUnLockStockOrder(OrderEntityVo vo);

}

