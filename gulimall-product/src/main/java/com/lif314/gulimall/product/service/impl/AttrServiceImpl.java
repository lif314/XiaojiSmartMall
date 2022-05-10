package com.lif314.gulimall.product.service.impl;

import com.lif314.common.constant.ProductConstant;
import com.lif314.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.lif314.gulimall.product.dao.AttrGroupDao;
import com.lif314.gulimall.product.dao.CategoryDao;
import com.lif314.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.lif314.gulimall.product.entity.AttrGroupEntity;
import com.lif314.gulimall.product.entity.CategoryEntity;
import com.lif314.gulimall.product.service.CategoryService;
import com.lif314.gulimall.product.vo.AttrGroupRelationVo;
import com.lif314.gulimall.product.vo.AttrRespVo;
import com.lif314.gulimall.product.vo.AttrVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.product.dao.AttrDao;
import com.lif314.gulimall.product.entity.AttrEntity;
import com.lif314.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;


@Service("AttrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {


    @Autowired
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存vo对象
     * <p>
     * 保存属性并添加到属性组关联表中
     */
    @Override
    public void saveAttr(AttrVo attrVo) {
        // 1 保存基本数据
        AttrEntity attrEntity = new AttrEntity();  // PO
        // 复制相同属性字段的值 source target
        BeanUtils.copyProperties(attrVo, attrEntity);
        this.save(attrEntity);

        // 2. 保存关联关系
        if (attrVo.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attrVo.getAttrGroupId() != null) { // 基本属性
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationEntity.setAttrGroupId(attrVo.getAttrGroupId());
            attrAttrgroupRelationDao.insert(attrAttrgroupRelationEntity);
        }

    }

    /**
     * 分类查询规格参数
     */
    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType) {
        // 查询条件 判断检索类型
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>().eq(
                "attr_type", "base".equalsIgnoreCase(attrType) ?
                        ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        ;

        // 查询所有的 id = 0
        if (catelogId != 0) {
            // 有分类条件下
            queryWrapper.eq("catelog_id", catelogId);
        }

        // 检索条件
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq("attr_name", key).or().like("attr_id", key);
            });
        }
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        /**
         * 还需要添加
         * 			"catelogName": "手机/数码/手机", //所属分类名字
         * 			"groupName": "主体", //所属分组名字
         */
        PageUtils pageUtils = new PageUtils(page);
        // 查询到的记录
        List<AttrEntity> records = page.getRecords();
        // 流水处理
        List<AttrRespVo> respVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);

            /**
             * 设置分类和分组的name
             */
            if ("base".equalsIgnoreCase(attrType)) { // 基本属性具有分组信息
                // 从关联表中查询分组id
                AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = attrAttrgroupRelationDao.selectOne(
                        new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));

                if (attrAttrgroupRelationEntity != null && attrAttrgroupRelationEntity.getAttrGroupId() != null) {
                    Long attrGroupId = attrAttrgroupRelationEntity.getAttrGroupId();

                    // 通过分组id查询name
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrGroupId);
                    if(attrGroupEntity != null){
                        attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                    }
                }
            }

            // 分类name
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(respVos);
        return pageUtils;
    }

    /**
     * 查询属性详情
     */
    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo respVo = new AttrRespVo();
        // 获取属性信息
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, respVo);

        // 基本属性才有分组信息
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 从关联表中查询分组id
            AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if (relationEntity != null) {
                respVo.setAttrGroupId(relationEntity.getAttrGroupId());
            }
        }

        // 分类信息
        Long[] catelogPath = categoryService.findCatelogPath(attrEntity.getCatelogId());
        if (catelogPath != null) {
            respVo.setCatelogPath(catelogPath);
        }
        return respVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attrVo) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attrVo, attrEntity);
        // 保存基本数据
        this.updateById(attrEntity);

        /**
         * 如果本来没有属性组，则是新增，需要判断是 新增还是更新
         */

        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            Long selectCount = attrAttrgroupRelationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrVo.getAttrId()));
            // 修改分组关联
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attrVo.getAttrGroupId());
            relationEntity.setAttrId(attrVo.getAttrId());
            if (selectCount > 0) {
                // 新增
                attrAttrgroupRelationDao.update(relationEntity, new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrVo.getAttrId()));
            } else {
                attrAttrgroupRelationDao.insert(relationEntity);
            }
        }
    }

    /**
     * 获取属性分组关联的基本属性
     */
    @Override
    public List<AttrEntity> getAttrRelation(Long attrgroupId) {
        // 先去关联表中查到所有的attr_id,再在attr表中查询
        List<AttrAttrgroupRelationEntity> entities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));

        // 收集id
        List<Long> attrIds = entities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        List<AttrEntity> attrEntities = new ArrayList<>();
        if (attrIds != null && attrIds.size() > 0) {
            attrEntities = this.listByIds(attrIds);
        }
        return attrEntities;
    }

    /**
     * 删除关联关系
     */
    @Override
    public void deleteRelation(AttrGroupRelationVo[] relationVos) {
//        attrAttrgroupRelationDao.delete(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", 1L).eq("attr_group_id", 1l));
        // 批量删除，只发送一次请求给数据库

        // 需要将vo转化为关系对象，因为Dao封装的就是关系对象，而vo是用来接受请求参数
        List<AttrAttrgroupRelationEntity> relationEntities = Arrays.asList(relationVos).stream().map((item) -> {
            AttrAttrgroupRelationEntity entity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, entity);
            return entity;
        }).collect(Collectors.toList());


        attrAttrgroupRelationDao.deleteBatchRelation(relationEntities);

    }

    /**
     * 获取属性分组里面还没有关联的本分类里面的其他基本属性，方便添加新的关联
     */
    @Override
    public PageUtils getAttrNoRelationAttr(Long attrgroupId, Map<String, Object> params) {
        // 1. 当前分组只能关联自己所属的分类里面的所有属性
        // 获取该分组的分类id
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();

        // 2. 当前分组只能关联别的分组没有引用的属性
        // 2.1 当前分类下的其它分组。并且只查询基本属性
        List<AttrGroupEntity> groupEntities = attrGroupDao.selectList(
                new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        // 获取id集合
        List<Long> groupIds = groupEntities.stream().map(AttrGroupEntity::getAttrGroupId).collect(Collectors.toList());
        // 2.2 这些分组关联的属性

        // 关联的属性集合
        List<AttrAttrgroupRelationEntity> attr_group = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", groupIds));
        // 获取关联的所有属性
        List<Long> attrIds = attr_group.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());

        // 2.3 从当前分类的所有属性中排除这些属性 -- 只查询基本属性
        QueryWrapper<AttrEntity> wrapper =
                new QueryWrapper<AttrEntity>().eq("catelog_id", catelogId).eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if(attrIds!= null && attrIds.size() > 0){
            wrapper.notIn("attr_id", attrIds);
        }

        // 模糊查询
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w -> {
                 w.eq("attr_id", key).or().like("attr_name", key);
            }));
        }

        // 分页查询
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);

        return new PageUtils(page);
    }

}
