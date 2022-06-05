package com.lif314.gulimall.ware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.constant.OrderConstant;
import com.lif314.common.constant.WareConstant;
import com.lif314.common.exception.NoStockException;
import com.lif314.common.to.SkuHasStockTo;
import com.lif314.gulimall.ware.to.StockDetailTo;
import com.lif314.gulimall.ware.to.StockLockedTo;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;
import com.lif314.common.utils.R;
import com.lif314.gulimall.ware.dao.WareOrderTaskDetailDao;
import com.lif314.gulimall.ware.dao.WareSkuDao;
import com.lif314.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.lif314.gulimall.ware.entity.WareOrderTaskEntity;
import com.lif314.gulimall.ware.entity.WareSkuEntity;
import com.lif314.gulimall.ware.feign.OrderFeignService;
import com.lif314.gulimall.ware.feign.ProductFeignService;
import com.lif314.gulimall.ware.service.WareOrderTaskDetailService;
import com.lif314.gulimall.ware.service.WareOrderTaskService;
import com.lif314.gulimall.ware.service.WareSkuService;
import com.lif314.gulimall.ware.vo.OrderEntityVo;
import com.lif314.gulimall.ware.vo.SkuItemLockTo;
import com.lif314.gulimall.ware.vo.SkuWareHasStock;
import com.lif314.gulimall.ware.vo.WareSkuLockVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;



@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    OrderFeignService orderFeignService;

    @Autowired
    WareOrderTaskDetailDao wareOrderTaskDetailDao;




    /**
     * 处理解锁库存请求
     */
    @Override
    public void handleUnLockStockWare(StockLockedTo locked) throws Exception {
        /**
         * 解锁
         *
         * 1、查询数据库中关于这个订单锁定库存的消息
         *  有：需要进行解锁
         *      解锁：
         *          1、没有这个订单，必须解锁
         *          2、有这个订单，不是解锁库存
         *              订单状态：已取消， 解锁库存
         *                        没有取消：不能解锁
         *  没有：库存锁定失败了，导致库存整体回滚，这种情况无需再解锁库存
         *
         *  只要解锁库存失败，就要告诉MQ解锁失败
         */
        StockDetailTo taskDetailTO = locked.getDetailTo();// 库存工作单详情TO
        WareOrderTaskDetailEntity taskDetail = wareOrderTaskDetailService.getById(taskDetailTO.getId());// 库存工作单详情Entity

        if (taskDetail != null) {
            // 1.工作单未回滚，需要解锁
            WareOrderTaskEntity task = wareOrderTaskService.getById(locked.getId());// 库存工作单Entity

            R r = orderFeignService.getOrderStatusByOrderSn(task.getOrderSn());// 订单Entity
            if (r.getCode() == 0) {
                // 订单数据返回成功
                Object data = r.get("data");
                String s = JSON.toJSONString(data);
                Integer status = Integer.getInteger(s);
                if (data == null || OrderConstant.OrderStatusEnum.CANCLED.getCode().equals(status)) {
                    // 2.订单已回滚 || 订单未回滚已取消状态
                    if (taskDetail.getLockStatus().equals(WareConstant.WareStockLockStatus.LOCKED.getStatus())) {
                        // 订单已锁定状态，需要解锁（消息确认）
                        unlockStockDB(taskDetailTO.getSkuId(), taskDetailTO.getWareId(), taskDetailTO.getSkuNum(), taskDetailTO.getId());
                    } else {
                        // 订单其他状态，不可解锁（消息确认）
                    }
                }
            } else {
                // 订单远程调用失败（消息重新入队）
                throw new RuntimeException("远程服务调用失败");
            }
        } else {
            // 3.无库存锁定工作单记录，已回滚，无需解锁（消息确认）
        }
    }

    /**
     * 重载
     * 订单关闭是关闭库存
     * 防止订单卡顿，导致订单状态一直没有更新，库存消息先到期，
     * 查订单状态为新建状态，没有进行解锁库存
     */
    @Override
    public void handleUnLockStockOrder(OrderEntityVo vo) {
        String orderSn = vo.getOrderSn();
        // 查询最新库存工作单的状态，防止重复解锁库存
        WareOrderTaskEntity task = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);

        Long taskId = task.getId();
        // 查询该工作单关联的所有 没有解锁的库存进行解锁
        List<WareOrderTaskDetailEntity> taskDetailEntities =  wareOrderTaskDetailService.getDetailsByTaskIdAndStatus(taskId, WareConstant.WareStockLockStatus.LOCKED.getStatus());

        for (WareOrderTaskDetailEntity entity : taskDetailEntities) {
            // 逐一进行库存的解锁
            unlockStockDB(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
        }
    }


    private void unlockStockDB(Long skuId, Long wareId, Integer num, Long detailToId) {
        // 取消状态才能取消  -- 订单不存在也需要解锁库存
        wareSkuDao.unlockStock(skuId, wareId, num);
        // 更新库存工作单的信息
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(detailToId);
        entity.setLockStatus(WareConstant.WareStockLockStatus.UNLOCKED.getStatus());
        wareOrderTaskDetailDao.updateById(entity);
    }


    /**
     * {
     * page: 1,//当前页码
     * limit: 10,//每页记录数
     * sidx: 'id',//排序字段
     * order: 'asc/desc',//排序方式
     * wareId: 123,//仓库id
     * skuId: 123//商品id
     * }
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId) && !"0".equalsIgnoreCase(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId) && !"0".equalsIgnoreCase(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 采购完成的入库
     */
    @Transactional
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 判断如果还没有这个库存记录，则需要新增
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            // 新增记录
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStockLocked(0);
            // 远程查询sku的名称   skuInfo
            // 我们没有必要因为没有仓库名字就让事务回滚，所以可以忽略没有仓库名的情况
            try {
                R info = productFeignService.info(skuId);
                if (info.getCode() == 0) {
                    Map<String, Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception e) {
                // 有错误不用管

                /**
                 * 有错误无需回滚
                 */
                // TODO 异常出现不回滚 -- 高级篇
            }
            wareSkuDao.insert(wareSkuEntity);
        } else {
            // 更新操作
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    /**
     * 远程查询是否有库存
     */
    @Override
    public List<SkuHasStockTo> getSkuHasStock(List<Long> skuIds) {

        return skuIds.stream().map((skuId) -> {
            SkuHasStockTo skuHasStockTo = new SkuHasStockTo();
            skuHasStockTo.setSkuId(skuId);
            // 每一个商品可能在不同仓库中，我们需要查的是总库存量
            // 总库存量=库存总量-锁定的库存(下单未发货)
            Long totalStock = baseMapper.getTotalStock(skuId);
            skuHasStockTo.setHasStock(totalStock != null && totalStock > 0);
            return skuHasStockTo;
        }).collect(Collectors.toList());
    }

    /**
     * 为某个订单锁定库存
     *
     * 默认只要时运行时以后长都会回滚
     *
     * 解锁库存的场景
     *  1、下订单成功，订单过期没有支付被系统自动取消，被用户手动取消。都要解锁库存
     *  2、下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚，之前锁定的
     *     库存都要自动解锁
     *
     */







    // 出现该异常就要回滚
//    @Transactional(rollbackFor = NoStockException.class)
    @Transactional
//    @Override
    public Boolean myOrderLockStock(WareSkuLockVo vo) {
        // TODO 按照下单地址，找到一个就近的仓库，锁定库存

        // 保存库存工作单 -- 追溯 回滚
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(taskEntity);

        // 1、找到每个商品在哪个仓库中都有库存
        // 先考虑单个库存的信息
        List<SkuItemLockTo> locks = vo.getLocks();

        List<SkuWareHasStock> hasStocks = locks.stream().map((item) -> {
            // 获取有存库的仓库，然后锁定
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            // 查询该商品在哪些仓库有库存
            List<Long> wareIds = wareSkuDao.listWatrIdHasStock(skuId);
            stock.setWareIds(wareIds);
            return stock;
        }).collect(Collectors.toList());

        Boolean allLock = true;
        // 2、锁定库存
        for (SkuWareHasStock hasStock : hasStocks) {
            // 该商品默认锁失败
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareIds();
            Integer num = hasStock.getNum();
            if (wareIds == null && wareIds.size() == 0) {
                throw new NoStockException(skuId);
            }
            /**
             * 1、如果每一个商品都锁定成功，将当前商品锁定了几件的详情记录发送给了mq
             * 2、如果锁定失败，前面保存的工作单信息就会回滚，因为这是一个事务。
             * 发送出去的消息，由于归滚后查不到指定的id，所以不用解锁 ==> 不合理！！！
             * 只发id不够，所以需要发送工作单的详情信息
             *
             */
            for (Long wareId : wareIds) {
                // 锁定库存 -- 成功返回1，否则就是0
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, num);
                if(count == 1){
                    skuStocked = true;
                    // TODO 告诉MQ库存锁定成功
                    // 保存锁库存信息表 -- 工作单详情
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", num, taskEntity.getId(), wareId,
                            WareConstant.WareStockLockStatus.LOCKED.getStatus());
                  wareOrderTaskDetailService.save(taskDetailEntity);

                  // 保存结束，发送给交换机 -- 锁成功一个，发送一个消息
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(taskEntity.getId()); // 工作单的id
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity, stockDetailTo);
                    stockLockedTo.setDetailTo(stockDetailTo); // 工作单详情, 防止前面的数据回滚后找不到数据
                    stockLockedTo.setOrderSn(taskEntity.getOrderSn());  // 设置订单号

                    // 发动消息
//                    String s = JSON.toJSONString(stockLockedTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                    break;
                }else{
                    // 锁失败：重试下一个仓库
                    // 根据工作详情处理库存中的锁定数量
                }
            }
            if(skuStocked == false){
                // 所有都没有锁住
                throw new NoStockException(skuId);
            }

        }
        // 全部锁成功
        return true;
    }


    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo lockTO) {
        // 按照收货地址找到就近仓库，锁定库存（暂未实现）
        // 采用方案：获取每项商品在哪些仓库有库存，轮询尝试锁定，任一商品锁定失败回滚

        // 1.往库存工作单存储当前锁定（本地事务表）
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(lockTO.getOrderSn());
        wareOrderTaskService.save(taskEntity);

        // 2.封装待锁定库存项Map
        Map<Long, SkuItemLockTo> lockItemMap = lockTO.getLocks().stream().collect(Collectors.toMap(key -> key.getSkuId(), val -> val));
        // 3.查询（库存 - 库存锁定 >= 待锁定库存数）的仓库
        List<WareSkuEntity> wareEntities = baseMapper.selectListHasSkuStock(lockItemMap.keySet()).stream().filter(entity -> entity.getStock() - entity.getStockLocked() >= lockItemMap.get(entity.getSkuId()).getCount()).collect(Collectors.toList());
        // 判断是否查询到仓库
        if (CollectionUtils.isEmpty(wareEntities)) {
            // 匹配失败，所有商品项没有库存
            Set<Long> skuIds = lockItemMap.keySet();
            throw new NoStockException(String.valueOf(skuIds));
        }
        // 将查询出的仓库数据封装成Map，key:skuId  val:wareId
        Map<Long, List<WareSkuEntity>> wareMap = wareEntities.stream().collect(Collectors.groupingBy(key -> key.getSkuId()));
        // 4.判断是否为每一个商品项至少匹配了一个仓库
        List<WareOrderTaskDetailEntity> taskDetails = new ArrayList<>();// 库存锁定工作单详情

        Map<Long, StockLockedTo> lockedMessageMap = new HashMap<>();// 库存锁定工作单消息
        if (wareMap.size() < lockTO.getLocks().size()) {
            // 匹配失败，部分商品没有库存
            Set<Long> skuIds = lockItemMap.keySet();
            skuIds.removeAll(wareMap.keySet());// 求商品项差集
            throw new NoStockException(String.valueOf(skuIds));
        } else {
            // 所有商品都存在有库存的仓库
            // 5.锁定库存
            for (Map.Entry<Long, List<WareSkuEntity>> entry : wareMap.entrySet()) {
                Boolean skuStocked = false;
                Long skuId = entry.getKey();// 商品
                SkuItemLockTo item = lockItemMap.get(skuId);
                Integer count = item.getCount();// 待锁定个数
                List<WareSkuEntity> hasStockWares = entry.getValue();// 有足够库存的仓库
                for (WareSkuEntity ware : hasStockWares) {
                    Long num = baseMapper.lockSkuStock(skuId, ware.getWareId(), count);
                    if (num == 1) {
                        // 锁定成功，跳出循环
                        skuStocked = true;

                        // 创建库存锁定工作单详情（每一件商品锁定详情）
                        WareOrderTaskDetailEntity taskDetail = new WareOrderTaskDetailEntity(null, skuId, "", count, taskEntity.getId(), ware.getWareId(),
                                WareConstant.WareStockLockStatus.LOCKED.getStatus());
                        taskDetails.add(taskDetail);

                        // 创建库存锁定工作单消息（每一件商品一条消息）
                        StockDetailTo detailMessage = new StockDetailTo();
                        BeanUtils.copyProperties(taskDetail, detailMessage);

                        StockLockedTo lockedMessage = new StockLockedTo();
                        lockedMessage.setDetailTo(detailMessage);
                        lockedMessage.setId(taskEntity.getId());
                        lockedMessage.setOrderSn(taskEntity.getOrderSn());
                        lockedMessageMap.put(skuId, lockedMessage);
                        break;
                    }
                }
                if (!skuStocked) {
                    // 匹配失败，当前商品所有仓库都未锁定成功
                    throw new NoStockException(skuId);
                }
            }
        }

        // 6.往库存工作单详情存储当前锁定（本地事务表）
        wareOrderTaskDetailService.saveBatch(taskDetails);

        // 7.发送消息
        for (WareOrderTaskDetailEntity taskDetail : taskDetails) {
            StockLockedTo message = lockedMessageMap.get(taskDetail.getSkuId());
            message.getDetailTo().setId(taskDetail.getId());// 存储库存详情ID
//            String s = JSON.toJSONString(message);
            rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", message);
        }

        return true;
    }

}
