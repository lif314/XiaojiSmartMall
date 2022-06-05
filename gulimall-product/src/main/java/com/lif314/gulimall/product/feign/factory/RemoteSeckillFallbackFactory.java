//package com.lif314.gulimall.product.feign.factory;
//
//import com.lif314.common.exception.BizCodeEnum;
//import com.lif314.common.utils.R;
//import com.lif314.gulimall.product.feign.SeckillFeignService;
//import feign.hystrix.FallbackFactory;
//import lombok.extern.slf4j.Slf4j;
////import org.springframework.cloud.openfeign.FallbackFactory;
//import org.springframework.stereotype.Component;
//
//
///**
// * 秒杀服务失败回调机制
// */
//
//@Slf4j
//@Component
//public class RemoteSeckillFallbackFactory implements FallbackFactory<SeckillFeignService> {
//
//    @Override
//    public SeckillFeignService create(Throwable throwable) {
//
//        log.error("秒杀服务调用失败:{}",throwable.getMessage());
//        return new SeckillFeignService()
//        {
//            @Override
//            public R getSeckillInfoBySkuId(Long skuId) {
//                System.out.println("熔断方法调用-------");
//                return R.error(BizCodeEnum.TOO_MANY_REQUESTS.getCode(), BizCodeEnum.TOO_MANY_REQUESTS.getMsg());
//            }
//        };
//    }
//}
