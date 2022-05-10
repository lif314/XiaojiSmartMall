package com.lif314.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.lif314.gulimall.product.dao.BrandDao;
import com.lif314.gulimall.product.dao.CategoryDao;
import com.lif314.gulimall.product.entity.BrandEntity;
import com.lif314.gulimall.product.entity.CategoryEntity;
import com.lif314.gulimall.product.service.BrandService;
import com.lif314.gulimall.product.vo.BrandVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.CategoryBrandRelationDao;
import com.lif314.gulimall.product.entity.CategoryBrandRelationEntity;
import com.lif314.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    // 注入Dao
    @Autowired
    CategoryDao categoryDao;

    @Autowired
    BrandDao brandDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存品牌分类关联的完整信息
     */
    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();
        // 查询品牌名和分类名
        BrandEntity brandEntity = brandDao.selectById(brandId);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        categoryBrandRelation.setBrandName(brandEntity.getName());
        categoryBrandRelation.setCatelogName(categoryEntity.getName());
        // 保存
        this.save(categoryBrandRelation);
    }

    /**
     * 级联更新
     */
    @Transactional
    @Override
    public void updateBrandName(Long brandId, String name) {
        CategoryBrandRelationEntity categoryBrandRelationEntity = new CategoryBrandRelationEntity();
        categoryBrandRelationEntity.setBrandId(brandId);
        categoryBrandRelationEntity.setBrandName(name);
        // 更新与更新条件
        this.update(categoryBrandRelationEntity, new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId));
    }

    @Transactional
    @Override
    public void updateCategoryName(Long catId, String name) {
        this.baseMapper.updateCategory(catId, name);
    }

    @Override
    public List<CategoryBrandRelationEntity> getBrandsByCatId(Long catId) {

        List<CategoryBrandRelationEntity> relationEntities = this.baseMapper.selectList(new QueryWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catId));

        return relationEntities;
    }

//    @Override
//    public List<BrandEntity> getBrandsByCatId(Long catId) {
//        // 在关联表中进行查询
//        List<CategoryBrandRelationEntity> relationEntities = relationDao.selectList(new QueryWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catId));
//        // 为了重用
//        List<BrandEntity> brandEntities = relationEntities.stream().map((item) -> {
//            Long brandId = item.getBrandId();
//            // 一般调用别的业务逻辑，使用别的Service
//            BrandEntity byId = brandService.getById(brandId);
//            return byId;
//
//        }).collect(Collectors.toList());
//
//        return brandEntities;
//    }


}
