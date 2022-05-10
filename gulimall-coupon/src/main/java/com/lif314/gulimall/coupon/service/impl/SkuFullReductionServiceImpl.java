package com.lif314.gulimall.coupon.service.impl;

import com.lif314.common.to.MemberPrice;
import com.lif314.common.to.SkuReductionTo;
import com.lif314.gulimall.coupon.entity.MemberPriceEntity;
import com.lif314.gulimall.coupon.entity.SkuLadderEntity;
import com.lif314.gulimall.coupon.service.MemberPriceService;
import com.lif314.gulimall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.coupon.dao.SkuFullReductionDao;
import com.lif314.gulimall.coupon.entity.SkuFullReductionEntity;
import com.lif314.gulimall.coupon.service.SkuFullReductionService;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Autowired
    SkuLadderService skuLadderService;

    @Autowired
    MemberPriceService memberPriceService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存满减优惠等信息
     */
    @Override
    public void saveFullReduction(SkuReductionTo skuReductionTo) {
        //  sku的优惠信息、满减信息 (跨库)
        /**
         * gulimall_sms->
         *   sms_sku_ladder
         *   sms_sku_full_reductio
         *   sms_member_price
         */

        // 存在优惠信息才保存
        if(skuReductionTo.getFullCount() > 0){
            SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
            skuLadderEntity.setSkuId(skuReductionTo.getSkuId());
            skuLadderEntity.setFullCount(skuReductionTo.getFullCount());
            skuLadderEntity.setDiscount(skuReductionTo.getDiscount());
            // 计算优惠的价格 -- 留在优惠系统中处理
//            skuLadderEntity.setPrice(new BigDecimal("0"));
            // 打折状态，是否还有其它优惠
            skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
            // 保存满减优惠信息
            skuLadderService.save(skuLadderEntity);
        }


        // 存在满减信息才进行保存
        if(skuReductionTo.getFullPrice() != null && skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) > 0){
            // 保存满减信息
            SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
            BeanUtils.copyProperties(skuReductionTo, skuFullReductionEntity);
            this.save(skuFullReductionEntity);
        }


        // 保存会员价格 -- 会员价格不为空
        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();
        if(memberPrice != null && memberPrice.size() > 0){
            List<MemberPriceEntity> priceEntities = memberPrice.stream().map((item) -> {
                MemberPriceEntity priceEntity = new MemberPriceEntity();
                priceEntity.setSkuId(skuReductionTo.getSkuId());
                priceEntity.setMemberLevelId(item.getId());// 会员等级id
                priceEntity.setMemberLevelName(item.getName());
                if (item.getPrice() != null) {
                    priceEntity.setMemberPrice(item.getPrice());
                }
                priceEntity.setAddOther(1); // 默认不叠加其它优惠
                return priceEntity;
            }).filter((item)->{
                // 过滤无意义的会员价格数据
                return item.getMemberPrice().compareTo(new BigDecimal("0")) > 0;
            }).collect(Collectors.toList());

            memberPriceService.saveBatch(priceEntities);
        }

    }

}
