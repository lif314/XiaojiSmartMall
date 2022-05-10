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

import com.lif314.gulimall.product.dao.SpuImagesDao;
import com.lif314.gulimall.product.entity.SpuImagesEntity;
import com.lif314.gulimall.product.service.SpuImagesService;


@Service("spuImagesService")
public class SpuImagesServiceImpl extends ServiceImpl<SpuImagesDao, SpuImagesEntity> implements SpuImagesService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuImagesEntity> page = this.page(
                new Query<SpuImagesEntity>().getPage(params),
                new QueryWrapper<SpuImagesEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存spu图片集
     */
    @Override
    public void saveImages(Long id, List<String> images) {

        // 判断是否为空
        if(images != null && images.size() > 0){
            List<SpuImagesEntity> collect = images.stream().map((img) -> {
                SpuImagesEntity entity = new SpuImagesEntity();
                entity.setSpuId(id);
                entity.setImgUrl(img);
                return entity;
            }).collect(Collectors.toList());

            // 批量保存
            this.saveBatch(collect);
        }

    }

}
