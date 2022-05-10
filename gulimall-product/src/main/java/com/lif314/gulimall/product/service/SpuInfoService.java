package com.lif314.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lif314.common.utils.PageUtils;
import com.lif314.gulimall.product.entity.SpuInfoEntity;
import com.lif314.gulimall.product.saveVo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:12:39
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo spuSaveVo);

    PageUtils queryPageByConditions(Map<String, Object> params);

    void upProduct(Long spuId);

    SpuInfoEntity getSpuInfoBySkuId(Long skuId);
}

