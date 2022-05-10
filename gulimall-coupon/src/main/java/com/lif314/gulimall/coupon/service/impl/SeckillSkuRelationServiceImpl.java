package com.lif314.gulimall.coupon.service.impl;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.coupon.dao.SeckillSkuRelationDao;
import com.lif314.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.lif314.gulimall.coupon.service.SeckillSkuRelationService;


@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    /**
     *
     * 查询条件
     * ?t=1648957478568&page=1&limit=10&key=121&promotionSessionId=1
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<SeckillSkuRelationEntity> wrapper = new QueryWrapper<>();

        // 模糊查询
        // key = 1 and (id=1 or spu_name like xxx)
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w) -> {
                w.eq("id", key).or().eq("promotion_id", key);
            });
        }

        String promotionSessionId = (String) params.get("promotionSessionId");
        if(!StringUtils.isEmpty(promotionSessionId)){
            wrapper.eq("promotion_session_id",  promotionSessionId);
        }

        IPage<SeckillSkuRelationEntity> page = this.page(
                new Query<SeckillSkuRelationEntity>().getPage(params),
               wrapper
        );

        return new PageUtils(page);
    }

}
