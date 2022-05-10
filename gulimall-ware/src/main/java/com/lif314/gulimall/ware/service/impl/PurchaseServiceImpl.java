package com.lif314.gulimall.ware.service.impl;

import com.lif314.common.constant.WareConstant;
import com.lif314.gulimall.ware.entity.PurchaseDetailEntity;
import com.lif314.gulimall.ware.service.PurchaseDetailService;
import com.lif314.gulimall.ware.service.WareSkuService;
import com.lif314.gulimall.ware.vo.PurchaseItemDoneVo;
import com.lif314.gulimall.ware.vo.MergeVo;
import com.lif314.gulimall.ware.vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.ware.dao.PurchaseDao;
import com.lif314.gulimall.ware.entity.PurchaseEntity;
import com.lif314.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {


    @Autowired
    PurchaseDetailService purchaseDetailService;

    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取未分配的采购单：新建  已分配
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageUnreceiveList(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();

        /**
         *            <el-option label="新建" :value="0"></el-option>
         *           <el-option label="已分配" :value="1"></el-option>
         *           <el-option label="已领取" :value="2"></el-option>
         *           <el-option label="已完成" :value="3"></el-option>
         *           <el-option label="有异常" :value="4"></el-option>
         */
        wrapper.eq("status", WareConstant.PurchaseStatusEnum.CREATED.getStatus())
                .or().eq("status", WareConstant.PurchaseStatusEnum.ASSSIGNED.getStatus());

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 合并采购单
     */
    @Transactional // 事务方法
    @Override
    public void mergeProcurements(MergeVo mergeVo) {

        Long purchaseId = mergeVo.getPurchaseId();
        if (purchaseId == null) {
            // 没有采购单，则新建一个采购单
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            // 设置一些默认值
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getStatus());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }

        // TODO 确定采购单状态是0和1的才能合并

        List<Long> items = mergeVo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = items.stream().map((id) -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(id);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            // 修改状态 --- 已分配
            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSSIGNED.getStatus());
            return purchaseDetailEntity;
        }).collect(Collectors.toList());

        // 批量保存
        purchaseDetailService.updateBatchById(collect);


        // 更新采购单的时间信息
        PurchaseEntity updatePurchase = new PurchaseEntity();
        updatePurchase.setId(purchaseId);
        updatePurchase.setUpdateTime(new Date());
        this.updateById(updatePurchase);

    }

    /**
     * 领取采购单
     */
    @Override
    public void received(List<Long> ids) {

        List<PurchaseEntity> collect = ids.stream().map((id) -> {
            // 获取采购单的详细信息
            PurchaseEntity byId = this.getById(id);
            return byId;
        }).filter((item) -> {
            // 过滤采购单
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getStatus() ||
                    item.getStatus() == WareConstant.PurchaseStatusEnum.ASSSIGNED.getStatus()) {
                return true;
            } else {
                return false;
            }
        }).map((update) -> {
            // 更新状态 -- 已领取
            update.setStatus(WareConstant.PurchaseStatusEnum.RECEIVED.getStatus());
            // 更新时间
            update.setUpdateTime(new Date());
            return update;
        }).collect(Collectors.toList());

        // 批量更新
        this.updateBatchById(collect);


        /**
         * 更新相关联的采购需求的状态 -- 正在采购
         *
         * 对于每一个采购单的id，收集与其相关的所有的采购需求id集合，设置
         * 新的状态，然后批量更新
         */
        ids.forEach((item) -> {
            List<PurchaseDetailEntity> purchaseDetailEntities = purchaseDetailService.listDetailByPurchaseId(item);
            List<PurchaseDetailEntity> detailEntities = purchaseDetailEntities.stream().map((entity) -> {
                // 更新状态 --- 正在采购
                PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
                // 只更新status
                detailEntity.setId(entity.getId());
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getStatus());
                return detailEntity;
            }).collect(Collectors.toList());

            // 批量更新
            purchaseDetailService.updateBatchById(detailEntities);
        });
    }

    /**
     * 完成采购单
     */
    @Override
    public void purchaseDone(PurchaseDoneVo purchaseDoneVo) {
        // 分离采购项 成功与 有异常
        Boolean flag = true;
        List<PurchaseItemDoneVo> items = purchaseDoneVo.getItems();
        List<PurchaseDetailEntity> updateItems = new ArrayList<>();
        for(PurchaseItemDoneVo item : items ) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.FAILURE.getStatus()) {
                flag = false;
                // 失败状态
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FAILURE.getStatus());
            }else{
                // 成功状态
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getStatus());

                // 根据id查询当前采购项的具体信息后添加到库存中
                PurchaseDetailEntity detailServiceById = purchaseDetailService.getById(item.getItemId());
                // 将成功采购的进行入库
                wareSkuService.addStock(detailServiceById.getSkuId(), detailServiceById.getWareId(), detailServiceById.getSkuNum());
            }
            // 添加需要更新的数据
            detailEntity.setId(item.getItemId());
            updateItems.add(detailEntity);
        };
        // 改变采购项的状态
        purchaseDetailService.updateBatchById(updateItems);

        // 改变采购单的状态 -- 需要所有采购项的状态完成才能标记为完成

        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseDoneVo.getId());
        purchaseEntity.setStatus(flag ? WareConstant.PurchaseStatusEnum.FINISH.getStatus() : WareConstant.PurchaseStatusEnum.HASERROR.getStatus());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);

    }

}
