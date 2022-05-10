package com.lif314.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lif314.common.constant.SecKillConstant;
import com.lif314.common.to.MemberRespTo;
import com.lif314.common.to.mq.SeckillOrderTo;
import com.lif314.common.utils.R;
import com.lif314.gulimall.seckill.feign.CouponFeignService;
import com.lif314.gulimall.seckill.feign.ProductFeignService;
import com.lif314.gulimall.seckill.intercepter.LoginUserInterceptor;
import com.lif314.gulimall.seckill.service.SecKillService;
import com.lif314.gulimall.seckill.to.SecKillSkuRedisTo;
import com.lif314.gulimall.seckill.vo.SeckillSessionVo;
import com.lif314.gulimall.seckill.to.SkuInfoTo;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class SecKillServiceImpl implements SecKillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;


    /**
     * 扫描最近三天内需要参与的秒杀活动
     */
    @Override
    public void updateSecKillSkuLatest3Days() {
        // 1、远程查询秒活动场次信息
        R r = couponFeignService.getLatest3DaysSession();
        if (r.getCode() == 0) {
            // 获取成功
            Object data = r.get("data");
            Object o = JSONObject.toJSON(data);
            String s = JSON.toJSONString(o);
            List<SeckillSessionVo> seckillSessionVos = JSON.parseObject(s, new TypeReference<List<SeckillSessionVo>>() {
            });

            // 上架商品，将商品存储在Redis中
            //  2、缓存活动信息
            saveSessionInfos(seckillSessionVos);
            // 3、缓存活动的关联信息
            saveRelationSkusInfos(seckillSessionVos);
        }
    }

    /**
     * 获取当前参与秒杀的商品信息
     */
    @Override
    public List<SecKillSkuRedisTo> getCurrentSecKillSkus() {
        // 1、确定当前时间是哪个场次
        long time = new Date().getTime();
        // 获取所有的场次信息
        Set<String> keys = redisTemplate.keys(SecKillConstant.SESSION_CACHE_PREFIX + "*");
        for (String key : keys) {
            String replace = key.replace(SecKillConstant.SESSION_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            Long start = Long.parseLong(s[0]);
            Long end = Long.parseLong(s[1]);
            if (time >= start && time < end) {
                // 2、获取场次下的秒杀商品
                List<String> range = redisTemplate.opsForList().range(key, 0, -1);// 获取该key关联的所有信息
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SecKillConstant.SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if (list != null) {
                    return list.stream().map((item) -> {
                        // 商品正在秒杀，需要随机码
                        return JSON.parseObject(item.toString(), SecKillSkuRedisTo.class);
                    }).collect(Collectors.toList());
                }
                break;
            }
        }
        return null;
    }

    /**
     * 查询商品的秒杀信息
     */
    @Override
    public SecKillSkuRedisTo getSkuSeckillInfo(Long skuId) {

        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SecKillConstant.SKUKILL_CACHE_PREFIX);
        // 所有参与秒杀的商品key信息
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                // 6_4
                if (Pattern.matches(regx, key)) {
                    String s = hashOps.get(key);
                    SecKillSkuRedisTo redisTo = JSON.parseObject(s, SecKillSkuRedisTo.class);
                    // 商品详情数据设为null，用不到
                    redisTo.setSkuInfoTo(null);
                    // 不能直接返回随机码
                    long time = new Date().getTime();
                    if (redisTo.getStartTime() <= time && time <= redisTo.getEndTime()) {
                        // 秒杀活动已经开始，可以返回随机码
                        return redisTo;
                    } else {
                        // 此时不能返回商品随机码
                        redisTo.setRandomCode(null);
                        return redisTo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 处理高并发秒杀
     *
     * @param killId     sessionId_skuId
     * @param randomCode 商品随机码
     * @param num        商品数量
     */
    @Override // 流量削峰
    public String kill(String killId, String randomCode, Integer num) {
        // 1、获取当前商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SecKillConstant.SKUKILL_CACHE_PREFIX);
        String s = hashOps.get(killId);
        if (!StringUtils.isEmpty(s)) {
            SecKillSkuRedisTo redisTo = JSON.parseObject(s, new TypeReference<SecKillSkuRedisTo>() {
            });
            // 校验合法性
            // 1、校验是否过期
            Long startTime = redisTo.getStartTime();
            Long endTime = redisTo.getEndTime();
            long time = new Date().getTime();
            if (time >= startTime && time <= endTime) {
                // 2、校验随机码是否正确
                String key = redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
                if (randomCode.equals(redisTo.getRandomCode()) && killId.equals(key)) {
                    // 3、检验购物数量是否合理
                    if (num <= redisTo.getSeckillLimit()) {
                        // 4、校验该用户是否已经买过了
                        // 幂等性处理：redis占位 userId_sessionId_skuId
                        MemberRespTo memberRespTo = LoginUserInterceptor.loginUser.get();
                        String userKey = SecKillConstant.USER_SECKILL_LIMIT + memberRespTo.getId() + "_" + redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
                        long ttl = endTime - time; // 超时时间 milliseconds
                        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent(userKey, num.toString(), ttl, TimeUnit.MILLISECONDS);// 不存在才占位,超时时间为场次结束时间
                        if (ifAbsent) {
                            // 占位成功，从来没有买过
                            // 减库存：减信号量
                            RSemaphore semaphore = redissonClient.getSemaphore(SecKillConstant.SKU_STOCK_SEMAPHORE + redisTo.getRandomCode());

                            // 非阻塞式的 -- 秒杀成功
                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                // 秒杀成功：发消息下订单
                                String orderSn = IdWorker.getTimeId();
                                // 返回订单号，说明秒杀ok，可以进行付款
                                // 发送消息
                                SeckillOrderTo seckillOrderTo = new SeckillOrderTo();
                                seckillOrderTo.setOrderSn(orderSn); // 订单号
                                seckillOrderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                                seckillOrderTo.setSeckillPrice(redisTo.getSeckillPrice());
                                seckillOrderTo.setMemberId(memberRespTo.getId());
                                seckillOrderTo.setNum(num);
                                seckillOrderTo.setSkuId(redisTo.getSkuId());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", seckillOrderTo);
                                return orderSn;
                            }

                        }

                    }
                }
            }


        }
        return null;
    }

    // 缓存活动信息
    private void saveSessionInfos(List<SeckillSessionVo> seckillSessionVos) {

        seckillSessionVos.forEach((sessionVo) -> {
            //时间当作Key
            Long startTime = sessionVo.getStartTime().getTime();
            Long endTime = sessionVo.getEndTime().getTime();
            String key = SecKillConstant.SESSION_CACHE_PREFIX + startTime + "_" + endTime;

            // 查询Redis中是否已经存在该活动
            Boolean hasKey = redisTemplate.hasKey(key);
            if (Boolean.FALSE.equals(hasKey)) {
                // 保存时需要保存场次信息和商品信息
                List<String> collect = sessionVo.getRelationSkus().stream().map((item) -> item.getPromotionSessionId().toString() + "_" + item.getSkuId().toString()).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, collect);
            }
        });

    }

    // 缓存活动中的商品信息
    private void saveRelationSkusInfos(List<SeckillSessionVo> seckillSessionVos) {

        seckillSessionVos.forEach((session) -> {
            // 准备Hasp操作
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SecKillConstant.SKUKILL_CACHE_PREFIX);

            session.getRelationSkus().forEach((skuRelationVo) -> {
                // 查看Redis中是否存在该商品，存在则不能重复上架
                Boolean hasKey = hashOps.hasKey(skuRelationVo.getPromotionSessionId().toString() + "_" + skuRelationVo.getSkuId().toString());
                if (Boolean.FALSE.equals(hasKey)) {
                    // 保存SKU详细信息
                    SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();
                    // 1、秒杀信息
                    BeanUtils.copyProperties(skuRelationVo, redisTo);
                    // 2、远程查询商品的详细信息
                    R r = productFeignService.getSkuInfo(skuRelationVo.getSkuId());
                    if (r.getCode() == 0) {
                        Object skuInfo = r.get("skuInfo");
                        String s = JSON.toJSONString(skuInfo);
                        SkuInfoTo skuInfoVo = JSON.parseObject(s, new TypeReference<SkuInfoTo>() {
                        });
                        redisTo.setSkuInfoTo(skuInfoVo);
                    }
                    // 3、当前商品的开始时间和结束时间
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    // 4、随机码：防止攻击！！秒杀开始的时刻才生成
                    String token = UUID.randomUUID().toString().replace("-", "");
                    redisTo.setRandomCode(token);

                    // 5、保存在Redis中
                    String s = JSON.toJSONString(redisTo);
                    // Key需要场次的信息   几场下的商品
                    hashOps.put(skuRelationVo.getPromotionSessionId().toString() + "_" + skuRelationVo.getSkuId().toString(), s);

                    // 如果当前这个场次的商品的库存信息已经上架就不需要上架

                    // 6、商品分布式信号量(自增量:限流)：高并发扣库存，只要进来一个请求，则信号量减一
                    // 减成功的才处理数据库
                    // 按照随机码进行减库存而不是按照商品ID，防止秒杀前恶意请求(已知商品ID)
                    // 使用redisson
                    RSemaphore semaphore = redissonClient.getSemaphore(SecKillConstant.SKU_STOCK_SEMAPHORE + token);
                    // 将存库设置为信号量，秒杀请求成功就减1
                    semaphore.trySetPermits(skuRelationVo.getSeckillCount());
                }
            });
        });
    }
}
