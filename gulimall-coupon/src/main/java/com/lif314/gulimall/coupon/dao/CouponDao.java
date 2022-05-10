package com.lif314.gulimall.coupon.dao;

import com.lif314.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:09:09
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
