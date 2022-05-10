package com.lif314.gulimall.product.dao;

import com.lif314.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lif314.gulimall.product.vo.SkuItemSaleAttrVo;
import com.lif314.gulimall.product.vo.SkuItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * sku销售属性&值
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:12:40
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    List<SkuItemSaleAttrVo> getSaleAttrsBySpuId(@Param("spuId") Long spuId);

    List<String> getSaleAttrsBySkuId(@Param("skuId") Long skuId);
}
