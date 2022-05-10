package com.lif314.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lif314.common.utils.PageUtils;
import com.lif314.gulimall.coupon.entity.MemberPriceEntity;

import java.util.Map;

/**
 * 商品会员价格
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:09:09
 */
public interface MemberPriceService extends IService<MemberPriceEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

