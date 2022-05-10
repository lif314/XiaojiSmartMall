package com.lif314.common.constant;

public class SecKillConstant {
    // 秒杀活动redis前缀
    public static final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    // 秒杀活动中商品信息
    public static final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    // 秒杀库存处理信号量
    public static final String SKU_STOCK_SEMAPHORE = "seckill:stock:"; // + 随机码

    // 上架秒杀商品幂等性保证--分布式锁
    public static final String SECKILL_UPLOAD_LOCK = "seckill:upload:lock";

    // 秒杀用户商品数量限制 -- 幂等性
    public static final String USER_SECKILL_LIMIT = "seckill:user:";
}
