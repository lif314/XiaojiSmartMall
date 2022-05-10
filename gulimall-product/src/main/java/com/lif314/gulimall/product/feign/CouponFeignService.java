package com.lif314.gulimall.product.feign;

import com.lif314.common.to.SkuReductionTo;
import com.lif314.common.to.SpuBoundTo;
import com.lif314.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 远程调用接口
 */

@FeignClient("gulimall-coupon") // 声明调用远程服务的名字
public interface CouponFeignService {

    /**
     * Feign基本原理：
     * 1、CouponFeignService.saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo)
     *      1)、@RequestBody将这个对象转换为json
     *      2）、找到gulimall-coupn服务，给coupon/spubounds/save发发送请求
     *          将上一步转的json数据放在请求体中，发送请求
     *     3）、对方服务收到请求，请求体中有json数据
     *          save(@RequestBody SpuBoundsEntity spuBounds):将请求体的json数据
     *          转为SpuBoundsEntity对象
     * 总结：只要json数据模型是兼容的(对象的属性名相同)。双方服务无需使用同一个To
     */

    /**
     * 新增积分信息（当前spu商品购买新增的积分规则信息）
     */
    @PostMapping("coupon/spubounds/save")  // 完整路径
    R saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);

    /**
     * 新增满减信息
     */
    @PostMapping("coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}
