package com.lif314.gulimall.product.service.impl;

import com.lif314.gulimall.product.dao.CategoryBrandRelationDao;
import com.lif314.gulimall.product.service.CategoryBrandRelationService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.BrandDao;
import com.lif314.gulimall.product.entity.BrandEntity;
import com.lif314.gulimall.product.service.BrandService;


@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {

    // 注入关联表中服务
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        // 获取key id或者名字
        String key = (String) params.get("key");
        // 封装查询条件
       QueryWrapper<BrandEntity> wrapper =  new QueryWrapper<BrandEntity>();
        if(!StringUtils.isEmpty(key)){
            wrapper.eq("brand_id",key).or().like("name", key);
        }
        // 加入查询条件
        IPage<BrandEntity> page = this.page(
                new Query<BrandEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 级联更新
     */
    @Override
    public void updateDetail(BrandEntity brand) {

        // 保证冗余字段数据的一致性
        this.updateById(brand);
        if(!StringUtils.isEmpty(brand.getName())){
            // 同步更新其它关联表中的数据
            categoryBrandRelationService.updateBrandName(brand.getBrandId(), brand.getName());

            // TODO 更新其它关联表
        }
    }

}
