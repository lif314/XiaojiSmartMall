package com.lif314.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lif314.common.utils.PageUtils;
import com.lif314.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.lif314.gulimall.product.vo.SkuItemSaleAttrVo;
import com.lif314.gulimall.product.vo.SkuItemVo;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:12:40
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<SkuItemSaleAttrVo> getSaleAttrsBySpuId(Long spuId);

    List<String> getSkuSaleAttrValuesBySkuId(Long skuId);
}

