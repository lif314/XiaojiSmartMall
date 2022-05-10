package com.lif314.gulimall.coupon.service.impl;

import com.lif314.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.lif314.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.coupon.dao.SeckillSessionDao;
import com.lif314.gulimall.coupon.entity.SeckillSessionEntity;
import com.lif314.gulimall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取最近三天内的秒杀活动
     */
    @Override
    public List<SeckillSessionEntity> getLatest3DaysSession() {
        // 1、开始时间 2022-3-12 00:00:00
        LocalDate now = LocalDate.now();
        LocalDateTime startTime = LocalDateTime.of(now, LocalTime.MIN);
        String formatStart = startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 2、结束时间 2022-3-14 23:59:59
        LocalDate plus2Days = now.plusDays(2);
        LocalDateTime endTime = LocalDateTime.of(plus2Days, LocalTime.MAX);
        String formatEnd = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 3、查询所有的活动
        List<SeckillSessionEntity> list = this.list(new QueryWrapper<SeckillSessionEntity>().between("start_time", formatStart, formatEnd));

        // 4、查询活动场次中的所有商品信息
        if(list != null && list.size() > 0){
            return list.stream().map((session) -> {
                List<SeckillSkuRelationEntity> promotionSession = seckillSkuRelationService.list(new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_session_id", session.getId()));
                session.setRelationSkus(promotionSession);
                return session;
            }).collect(Collectors.toList());
        }
        return null;
    }

}
