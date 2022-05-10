package com.lif314.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;
import com.lif314.gulimall.product.dao.AttrGroupDao;
import com.lif314.gulimall.product.entity.AttrEntity;
import com.lif314.gulimall.product.entity.AttrGroupEntity;
import com.lif314.gulimall.product.service.AttrGroupService;
import com.lif314.gulimall.product.service.AttrService;
import com.lif314.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.lif314.gulimall.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {


    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        // 查询条件
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if(!StringUtils.isEmpty(key)){
            wrapper.and((obj)->{
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }

        // id为0，查询所有
        if(catelogId == 0){
            /**
             * Query里面就有个方法getPage()，传入map，将map解析为mybatis-plus的IPage对象
             * 自定义PageUtils类用于传入IPage对象，得到其中的分页信息
             * AttrGroupServiceImpl extends ServiceImpl，其中ServiceImpl的父类中有方法
             * page(IPage, Wrapper)。对于wrapper而言，没有条件的话就是查询所有
             * queryPage()返回前还会return new PageUtils(page);，把page对象解析好页码信
             * 息，就封装为了响应数据
             */
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    wrapper
            );
            // 返回分类数据
            return new PageUtils(page);
        }else{
            // 只有不是全部查询时才考虑id
            wrapper.eq("catelog_id", catelogId);
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params)
                    , wrapper);
            return  new PageUtils(page);
        }
    }

    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
        // 1 查出分类下的所有属性分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        // 2 查出属性分组下的所有属性
        List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().map((group) -> {
            AttrGroupWithAttrsVo attrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(group, attrsVo);
            // 查询所有属性
            List<AttrEntity> attrs = attrService.getAttrRelation(group.getAttrGroupId());
                attrsVo.setAttrs(attrs);
            return attrsVo;
        }).collect(Collectors.toList());

        return collect;
    }

    /**
     * 查出当前spuId对应的所有属性的分组信息以及当前分组下所有属性对应的值
     * @param spuId spu id
     * @return 分组信息与分组下的属性信息
     */
    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        // 1.通过spuId查询所有属性值（pms_product_attr_value）
        // 2.通过attrId关联所有属性分组（pms_attr_attrgroup_relation）
        // 3.通过attrGroupId + catalogId关联属性分组名称（pms_attr_group）
        return baseMapper.getAttrGroupWithAttrsBySpuId(spuId, catalogId);
    }


}
