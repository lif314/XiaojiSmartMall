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
     * sku检索
     *
     * {
     * page: 1,//当前页码
     * limit: 10,//每页记录数
     * sidx: 'id',//排序字段
     * order: 'asc/desc',//排序方式
     * key: '华为',//检索关键字
     * catelogId: 0,
     * brandId: 0,
     * min: 0,
     * max: 0
     * }
     */
    @Override
    public PageUtils queryPageByConditions(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();

        // 模糊查询
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and((w)->{ // and括起or条件
                w.eq("sku_id", key).or().like("sku_name", key);
            });
        }

        // 三级分类
        String catelogId = (String) params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id", catelogId);
        }

        // 品牌
        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id", brandId);
        }

        // 价格区间

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
     * 通过spuid查询出所有的skus信息
     * @param spuId
     * @return
     */
    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        return this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
    }

    /**
     * 查询商品详情
     * @param skuId 商品id
     * @return 商品详情
     */
    @Override
    public SkuItemVo item(Long skuId) {
        /**
         * 1、sku基本信息【标题、副标题、价格】pms_sku_info
         * 2、sku图片信息【每个sku_id对应了多个图片】pms_sku_images
         * 3、spu下所有sku销售属性组合【不只是当前sku_id所指定的商品】
         * 4、spu商品介绍
         * 5、spu规格与包装【参数信息】
         */
        SkuItemVo skuItemVo = new SkuItemVo();

        // 异步编排
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            // 1、sku基本信息【标题、副标题、价格】pms_sku_info
            SkuInfoEntity skuInfo = getById(skuId);
            skuItemVo.setInfo(skuInfo);
            return skuInfo;
        }, executor);

        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            // 3、spu下所有sku销售属性组合【不只是当前sku_id所指定的商品】
            List<SkuItemSaleAttrVo> saleAttrVos = skuSaleAttrValueService.getSaleAttrsBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(saleAttrVos);
        }, executor);

        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync((res) -> {
            // 4、spu商品介绍
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesc(spuInfoDesc);
        }, executor);

        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync((res) -> {
            // 5、spu规格与包装【参数信息】
            List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrs(attrGroupVos);
        }, executor);


        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            // 2、sku图片信息【每个sku_id对应了多个图片】pms_sku_images
            List<SkuImagesEntity> images = imagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(images);
        }, executor);

        // 查询当前商品的秒杀信息
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

        // 等待所有任务都完成
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
     * 获取选中商品的最新价格信息
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
