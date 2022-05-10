package com.lif314.gulimall.seckill.service;


import com.lif314.gulimall.seckill.to.SecKillSkuRedisTo;

import java.util.List;

public interface SecKillService {
    void updateSecKillSkuLatest3Days();

    List<SecKillSkuRedisTo> getCurrentSecKillSkus();

    SecKillSkuRedisTo getSkuSeckillInfo(Long skuId);

    String kill(String killId, String randomCode, Integer num);
}
