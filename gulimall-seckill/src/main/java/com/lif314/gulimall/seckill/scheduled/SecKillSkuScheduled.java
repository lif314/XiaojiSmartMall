package com.lif314.gulimall.seckill.scheduled;


import com.lif314.common.constant.SecKillConstant;
import com.lif314.gulimall.seckill.service.SecKillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


/**
 * 秒杀商品定时上架：
 *      每天晚上三点上架最近三天内要秒杀的商品
 *      当天00:00:00 - 23:59:59
 *      明天00:00:00 - 23:59:59
 *      后天00:00:00 - 23:59:59
 */
@Service
@Slf4j
public class SecKillSkuScheduled {

    @Autowired
    SecKillService secKillService;


    @Autowired
    RedissonClient redissonClient;

    @Scheduled(cron = "0 0 3 * * ?") // 每天3点执行一次
//    @Scheduled(cron = "0/5 * * * * ?")  // 测试：每5秒执行一次
    public void updateSecKillSkuLatest3Days(){
        // TODO 重复上架无需处理(×) --> 接口幂等性

        // 使用分布式锁解决不同机器同时执行定时任务的问题
        RLock lock = redissonClient.getLock(SecKillConstant.SKU_STOCK_SEMAPHORE);
        lock.lock(10, TimeUnit.SECONDS);  // 10s后释放
        try{
            secKillService.updateSecKillSkuLatest3Days();
        }finally {
            // 无论执行结果如何，需要进行解锁
            lock.unlock();
        }
    }
}
