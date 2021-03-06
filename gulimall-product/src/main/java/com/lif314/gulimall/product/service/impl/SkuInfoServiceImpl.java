package com.lif314.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lif314.common.utils.R;
import com.lif314.gulimall.product.entity.*;
import com.lif314.gulimall.product.feign.SeckillFeignService;
import com.lif314.gulimall.product.service.*;
import com.lif314.gulimall.product.to.SecKillSkuRedisTo;
import com.lif314.gulimall.product.vo.CartItemPriceMapVo;
import com.lif314.gulimall.product.vo.SkuItemSaleAttrVo;
import com.lif314.gulimall.product.vo.SkuItemVo;
import com.lif314.gulimall.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.SkuInfoDao;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService imagesService;

    @Autowired
    SpuInfoDescService spuInfoDescService;


    @Autowired
    AttrGroupService attrGroupService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    ThreadPoolExecutor executor;


    @Autowired
    SeckillFeignService seckillFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }


    /**
     * sku??????
     *
     * {
     * page: 1,//????????????
     * limit: 10,//???????????????
     * sidx: 'id',//????????????
     * order: 'asc/desc',//????????????
     * key: '??????',//???????????????
     * catelogId: 0,
     * brandId: 0,
     * min: 0,
     * max: 0
     * }
     */
    @Override
    public PageUtils queryPageByConditions(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();

        // ????????????
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and((w)->{ // and??????or??????
                w.eq("sku_id", key).or().like("sku_name", key);
            });
        }

        // ????????????
        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id", catelogId);
        }

        // ??????
        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id", brandId);
        }

        // ????????????

        String min = (String) params.get("min");
        if(!StringUtils.isEmpty(min)){
            wrapper.ge("price", min);
        }

        String max = (String) params.get("max");
        if(!StringUtils.isEmpty(max)){
            try{
                BigDecimal bigDecimal = new BigDecimal(max);
                if(bigDecimal.compareTo(new BigDecimal("0")) > 0){
                    wrapper.le("price", max);
                }
            }catch(Exception e){

            }
        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * ??????spuid??????????????????skus??????
     * @param spuId
     * @return
     */
    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        return this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
    }

    /**
     * ??????????????????
     * @param skuId ??????id
     * @return ????????????
     */
    @Override
    public SkuItemVo item(Long skuId) {
        /**
         * 1???sku?????????????????????????????????????????????pms_sku_info
         * 2???sku?????????????????????sku_id????????????????????????pms_sku_images
         * 3???spu?????????sku????????????????????????????????????sku_id?????????????????????
         * 4???spu????????????
         * 5???spu?????????????????????????????????
         */
        SkuItemVo skuItemVo = new SkuItemVo();

        // ????????????
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            // 1???sku?????????????????????????????????????????????pms_sku_info
            SkuInfoEntity skuInfo = getById(skuId);
            skuItemVo.setInfo(skuInfo);
            return skuInfo;
        }, executor);

        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            // 3???spu?????????sku????????????????????????????????????sku_id?????????????????????
            List<SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(saleAttrVos);
        }, executor);

        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
            // 4???spu????????????
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesc(spuInfoDesc);
        }, executor);

        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            // 5???spu?????????????????????????????????
            List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrs(attrGroupVos);
        }, executor);


        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            // 2???sku?????????????????????sku_id????????????????????????pms_sku_images
            List<SkuImagesEntity> images = imagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(images);
        }, executor);

        // ?????????????????????????????????
        CompletableFuture<Void> seckillInfoFuture = CompletableFuture.runAsync(() -> {
            R r = seckillFeignService.getSeckillInfoBySkuId(skuId);
            if (r.getCode() == 0) {
                Object data = r.get("data");
                String s = JSON.toJSONString(data);
                SecKillSkuRedisTo secKillSkuRedisTo = JSON.parseObject(s, new TypeReference<SecKillSkuRedisTo>() {
                });
                skuItemVo.setSeckillInfo(secKillSkuRedisTo);
            }
        }, executor);

        // ???????????????????????????
        try {
            CompletableFuture.allOf(saleAttrFuture, descFuture, baseAttrFuture, imageFuture, seckillInfoFuture).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return skuItemVo;
    }

    /**
     * ???????????????????????????????????????
     */
    @Override
    public CartItemPriceMapVo getCartItemNewPrices(List<Long> ids) {
        List<SkuInfoEntity> infoEntities = this.baseMapper.selectBatchIds(ids);
        Map<Long, BigDecimal> maps = new HashMap<>();
        for (SkuInfoEntity entity : infoEntities) {
            maps.put(entity.getSkuId(), entity.getPrice());
        }

        CartItemPriceMapVo cartItemPriceMapVo = new CartItemPriceMapVo();
        cartItemPriceMapVo.setItemNewPrice(maps);
        return cartItemPriceMapVo;
    }

}
