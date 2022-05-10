package com.lif314.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lif314.common.to.SkuReductionTo;
import com.lif314.common.utils.PageUtils;
import com.lif314.gulimall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:09:09
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveFullReduction(SkuReductionTo skuReductionTo);
}

