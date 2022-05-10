package com.lif314.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.R;
import com.lif314.gulimall.ware.entity.PurchaseEntity;
import com.lif314.gulimall.ware.vo.MergeVo;
import com.lif314.gulimall.ware.vo.PurchaseDoneVo;

import java.util.List;
import java.util.Map;

/**
 * 采购信息

 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:20:38
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnreceiveList(Map<String, Object> params);

    void mergeProcurements(MergeVo mergeVo);

    void received(List<Long> ids);

    void purchaseDone(PurchaseDoneVo purchaseDoneVo);
}
