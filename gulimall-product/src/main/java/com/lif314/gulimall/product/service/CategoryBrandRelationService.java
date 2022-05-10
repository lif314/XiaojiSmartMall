package com.lif314.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lif314.common.utils.PageUtils;
import com.lif314.gulimall.product.entity.BrandEntity;
import com.lif314.gulimall.product.entity.CategoryBrandRelationEntity;
import com.lif314.gulimall.product.entity.CategoryEntity;
import com.lif314.gulimall.product.vo.BrandVo;

import java.util.List;
import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:12:40
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveDetail(CategoryBrandRelationEntity categoryBrandRelation);

    void updateBrandName(Long brandId, String name);

    void updateCategoryName(Long catId, String name);

    List<CategoryBrandRelationEntity> getBrandsByCatId(Long catId);
//    List<BrandEntity> getBrandsByCatId(Long catId);
}

