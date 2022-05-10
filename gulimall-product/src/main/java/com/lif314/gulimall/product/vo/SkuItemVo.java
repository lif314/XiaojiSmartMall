package com.lif314.gulimall.product.vo;

import com.lif314.gulimall.product.entity.SkuImagesEntity;
import com.lif314.gulimall.product.entity.SkuInfoEntity;
import com.lif314.gulimall.product.entity.SpuInfoDescEntity;
import com.lif314.gulimall.product.to.SecKillSkuRedisTo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品详情vo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuItemVo {

    /**
     * 1、sku基本信息【标题、副标题、价格】pms_sku_info
     * 2、sku图片信息【每个sku_id对应了多个图片】pms_sku_images
     * 3、spu下所有sku销售属性组合【不只是当前sku_id所指定的商品】
     * 4、spu商品介
     * 5、spu规格与包装【参数信息】
     */

    // 1、sku基本信息（pms_sku_info）【默认图片、标题、副标题、价格】
    private SkuInfoEntity info;

    private boolean hasStock = true;// 是否有货

    //2、sku图片信息（pms_sku_images）图片集
    private List<SkuImagesEntity> images;

    //3、当前sku所属spu下的所有销售属性组合（pms_sku_sale_attr_value）
    private List<SkuItemSaleAttrVo> saleAttr;

    //4、spu商品介绍（pms_spu_info_desc）共享SPU信息 【描述图片】
    private SpuInfoDescEntity desc; // 图片URL以,分割

    //5、spu规格参数信息(规格与包装)（pms_attr）【以组为单位】销售属性对应的值情况
    private List<SpuItemAttrGroupVo> groupAttrs;

    // 6、商品秒杀信息
    private SecKillSkuRedisTo seckillInfo;
}
