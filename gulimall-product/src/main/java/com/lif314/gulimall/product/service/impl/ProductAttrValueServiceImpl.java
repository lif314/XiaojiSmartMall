package com.lif314.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.ProductAttrValueDao;
import com.lif314.gulimall.product.entity.ProductAttrValueEntity;
import com.lif314.gulimall.product.service.ProductAttrValueService;
import org.springframework.transaction.annotation.Transactional;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取SPU规格
     */
    @Override
    public List<ProductAttrValueEntity> listForSpu(Long spuId) {

        return this.baseMapper.selectList(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
    }

    /**
     * 修改商品规格
     */
    @Transactional
    @Override
    public void updateSpuAttrs(Long spuId, List<ProductAttrValueEntity> entities) {
        /**
         * 为什么不直接更新呢？
         *
         * 前端有可能删除属性，传回后端的数据只有修改的属性，所以需要删除原有属性
         *
         * 如果更新的话，需要考虑新增的规格、删除多余的规格、更新现有的规格，比较麻烦
         *
         * 因为spu属性只和spuId相关联，所以可以删除后保存新的。
         * 如果存在其它关联，则只能进行更新
         */
        // 删除spuId相关联的所有属性
        this.baseMapper.delete(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
        // 插入
        List<ProductAttrValueEntity> collect = entities.stream().map(item -> {
            item.setId(null);    // 消除原有的id信息
            item.setSpuId(spuId);
            return item;
        }).collect(Collectors.toList());
        // 批量新增
        this.saveBatch(collect);
    }

    /**
     * 将可检索的规格属性的Id返回
     */
    @Override
    public List<Long> selectSearchAttrs(List<Long> attrIds) {
        // SELECT attr_id FROM `pms_attr` WHERE attr_id IN(?) AND search_type = 1
        return baseMapper.selectSearchAttrs(attrIds);
    }

}
