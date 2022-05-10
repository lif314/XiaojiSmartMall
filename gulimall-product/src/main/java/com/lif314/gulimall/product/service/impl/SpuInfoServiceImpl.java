package com.lif314.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lif314.common.constant.ProductConstant;
import com.lif314.common.to.SkuHasStockTo;
import com.lif314.common.to.SkuReductionTo;
import com.lif314.common.to.SpuBoundTo;
import com.lif314.common.to.es.SkuEsModel;
import com.lif314.common.utils.R;
import com.lif314.gulimall.product.entity.*;
import com.lif314.gulimall.product.feign.CouponFeignService;
import com.lif314.gulimall.product.feign.SearchFeignService;
import com.lif314.gulimall.product.feign.WareFeignService;
import com.lif314.gulimall.product.saveVo.*;
import com.lif314.gulimall.product.service.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;


    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    // 注入远程调用服务
    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional  // 大保存
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {

        /**
         * 1、保存spu基本信息 pms_spu_info
         */
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.baseMapper.insert(spuInfoEntity);

        /**
         * 2、保存spu的描述图片 pms_spu_info_desc
         */
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        // 插入有会将id封装在其中 --- 回填数据 -- 主键回填机制
        descEntity.setSpuId(spuInfoEntity.getId());
        // 拼接描述信息
        descEntity.setDecript(String.join(",", decript));
        spuInfoDescService.save(descEntity);

        /**
         * 3、保存SPU的图片集 pms_spu_images
         */
        List<String> images = vo.getImages();
        spuImagesService.saveImages(spuInfoEntity.getId(), images);

        /**
         * 4、 保存spu的规格参数 pms_product_attr_value
         */
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map((attr) -> {
            ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();
            productAttrValueEntity.setSpuId(spuInfoEntity.getId());
            productAttrValueEntity.setAttrId(attr.getAttrId());
            AttrEntity attrEntity = attrService.getById(attr.getAttrId());
            productAttrValueEntity.setAttrName(attrEntity.getAttrName());
            productAttrValueEntity.setAttrValue(attr.getAttrValues());
            productAttrValueEntity.setQuickShow(attr.getShowDesc());
            return productAttrValueEntity;
        }).collect(Collectors.toList());
        productAttrValueService.saveBatch(collect);


        /**
         * 5、保存spu的积分信息：sms_spu_bounds
         */
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r.getCode() != 0) { // 请求失败
            log.error("远程保存spu优惠信息失败！");
        }

        /**
         * 6、保存当前spu的对应的sku信息:
         */
        List<Skus> skus = vo.getSkus();
        if (skus != null && skus.size() > 0) {
            skus.forEach((item) -> {

                // 寻找默认图片
                String defaultImg = "";
                for (Images image : item.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();
                    }
                }

                // 6.1 sku的基本信息 pms_sku_info
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
//                private String skuName;
//                private BigDecimal price;
//                private String skuTitle;
//                private String skuSubtitle;
                // 拷贝只会拷贝基本属性的值
                BeanUtils.copyProperties(item, skuInfoEntity);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                // 设置默认图片：该属性应该先遍历图片找到字段为1的图片
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                skuInfoService.save(skuInfoEntity);

                // 主键回填
                Long skuId = skuInfoEntity.getSkuId();

                // 6.2 sku的图片信息 pms_sku_images
                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map((img) -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter((entity) -> {
                    //过滤空的图片：true是需要，false是剔除
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());
                skuImagesService.saveBatch(imagesEntities);
                // TODO: 没有图片，路径无需保存

                // 6.3 sku的销售属性信息 pms_sku_sale_attr_value
                List<Attr> attr = item.getAttr();
                List<SkuSaleAttrValueEntity> attrValueEntities = attr.stream().map((attrValue) -> {
                    SkuSaleAttrValueEntity attrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attrValue, attrValueEntity);
                    attrValueEntity.setSkuId(skuId);
                    return attrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(attrValueEntities);

                // 6.4 sku的优惠信息、满减信息 (跨库)
                /**
                 * gulimall_sms->
                 *   sms_sku_ladder
                 *   sms_sku_full_reductio
                 *   sms_member_price
                 */
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                // 剔除没有意义的满减信息
                // 存在打折数据 || 存在满减信息 || 存在会员优惠价格信息
                if (skuReductionTo.getFullCount() > 0 || (skuReductionTo.getFullPrice() != null && skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) > 0) || (skuReductionTo.getMemberPrice() != null && skuReductionTo.getMemberPrice().size() > 0)) {
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r1.getCode() != 0) {
                        log.error("远程保存spu优惠信息、满减信息失败！");
                    }
                }
            });
        }
    }

    /**
     * Spu检索
     * {
     * page: 1,//当前页码
     * limit: 10,//每页记录数
     * sidx: 'id',//排序字段
     * order: 'asc/desc',//排序方式
     * key: '华为',//检索关键字
     * catelogId: 6,//三级分类id
     * brandId: 1,//品牌id
     * status: 0,//商品状态
     * }
     * <p>
     * 检索条件：
     * - 模糊查询 key
     * - 三级分类 catelogId
     * - 品牌 brandId
     * - 商品状态 status
     */
    @Override
    public PageUtils queryPageByConditions(Map<String, Object> params) {

        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        // 模糊查询
        // status = 1 and (id=1 or spu_name like xxx)
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w) -> {
                w.eq("id", key).or().like("spu_name", key);
            });
        }

        // 三级分类
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }

        // 品牌
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }

        // 商品状态
        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)) {
            wrapper.eq("publish_status", status);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    /**
     * 商品上架
     */
    @Override
    public void upProduct(Long spuId) {

        /**
         *   组装数据
         */
        // 【1】 查出当前spuid对应的所有的sku信息，品牌的名字等
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);

        // TODO 查询当前sku的所有【可以被检索】规格属性
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.listForSpu(spuId);
        // 规格属性Id
        List<Long> attrIds = baseAttrs.stream().map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());
        // 查询所有可以被检索的规格属性
        List<Long> searchAttrs = productAttrValueService.selectSearchAttrs(attrIds);
        Set<Long> idSet = new HashSet<>(searchAttrs);

        List<SkuEsModel.Attrs> attrsList = baseAttrs.stream().filter((item) -> {
            return idSet.contains(item.getAttrId());
        }).map((item) -> {
            SkuEsModel.Attrs attrs1 = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs1);
            return attrs1;
        }).collect(Collectors.toList());

        // 一次查询是否有库存 -- 远程调用可能失败
        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        Map<Long, Boolean> stockMap = null;
        try {
            R skuHasStock = wareFeignService.getSkuHasStock(skuIdList);
            // 转换成map
            String s = JSON.toJSONString(skuHasStock.get("data"));
            TypeReference<List<SkuHasStockTo>> listTypeReference = new TypeReference<List<SkuHasStockTo>>(){};
            List<SkuHasStockTo> skuHasStockTos = JSON.parseObject(s, listTypeReference);
            stockMap = skuHasStockTos.stream().collect(Collectors.toMap(SkuHasStockTo::getSkuId, SkuHasStockTo::getHasStock));
        } catch (Exception e) {
            log.error("库存查询出现异常：{}", e);
        }


        Map<Long, Boolean> finalStockMap = stockMap;

        // 【2】 封装每一个sku信息
        List<SkuEsModel> upProducts = skus.stream().map((sku) -> {
            // 组装需要的数据
            SkuEsModel esModel = new SkuEsModel();
            // 相同属性值进行对拷
            BeanUtils.copyProperties(sku, esModel);

            // 不同属性的值重新处理 价格 图片
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());

            // 热度评分 hasStore: TODO 默认0
            esModel.setHotScore(0L);

            // TODO 品牌和分类的名字信息
            // 品牌信息
            BrandEntity brand = brandService.getById(sku.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());
            // 分类信息
            CategoryEntity category = categoryService.getById(sku.getCatalogId());
            esModel.setCatalogName(category.getName());

            // 设置检索 -- 规格属性 -- 只需要查询一次
            esModel.setAttrs(attrsList);

            // 是否拥有库存 hasStock： TODO gulimall-ware发送询问
            if (finalStockMap == null) {
                // 查询失败，默认为true
                esModel.setHasStock(true);
            } else {
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }

            return esModel;
        }).collect(Collectors.toList());


        // TODO 发送给ES进行保存 gulimall-search
        R r = searchFeignService.productStatusUp(upProducts);
        if (r.getCode() == 0) {
            // 远程调用成功
            // TODO 修改当前spu状态
            SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
            spuInfoEntity.setId(spuId);
            spuInfoEntity.setPublishStatus(ProductConstant.ProductStatusEnum.SPU_UP.getCode());
            spuInfoEntity.setUpdateTime(new Date());
            this.baseMapper.updateById(spuInfoEntity);
        } else {
            // 远程调用失败
            // TODO 重复调用 接口幂等性 远程调用重试机制
        }
    }

    /**
     * 根据skuid获取spu信息
     */
    @Override
    public SpuInfoEntity getSpuInfoBySkuId(Long skuId) {
        SkuInfoEntity skuInfo = skuInfoService.getById(skuId);
        SpuInfoEntity spuInfo = this.getById(skuInfo.getSpuId());
        return spuInfo;
    }

}
