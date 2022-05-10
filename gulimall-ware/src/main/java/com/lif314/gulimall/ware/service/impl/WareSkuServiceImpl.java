package com.lif314.gulimall.ware.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.constant.OrderConstant;
import com.lif314.common.constant.WareConstant;
import com.lif314.common.exception.NoStockException;
import com.lif314.common.to.SkuHasStockTo;
import com.lif314.common.to.mq.StockDetailTo;
import com.lif314.common.to.mq.StockLockedTo;
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

import java.util.List;
import java.util.Map;
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
    public void handleUnLockStockWare(StockLockedTo to) {
        Long id = to.getId();  // 库存工作单id
        StockDetailTo detailTo = to.getDetailTo();
        Long detailToId = detailTo.getId();  // 工作单详情id
        Long skuId = detailTo.getSkuId();
        Long wareId = detailTo.getWareId();
        Integer num = detailTo.getSkuNum();
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
        WareOrderTaskDetailEntity taskDetailEntity = wareOrderTaskDetailService.getById(detailToId);
        // 库存工作单状态为未解锁才能进行解锁
        if(taskDetailEntity != null && taskDetailEntity.getLockStatus().compareTo(WareConstant.WareStockLockStatus.LOCKED.getStatus()) == 0){
            // 需要进行解锁  -- 商品数据加上
            // 根据订单号查询订单状态信息
            String orderSn = to.getOrderSn();
            R r = orderFeignService.getOrderStatusByOrderSn(orderSn);
            if(r.getCode() == 0){
                // 订单状态获取成功 Integer status
                Object data = r.get("data");
                String s = JSON.toJSONString(data);
                Integer status = Integer.getInteger(s);

                if(data == null || status.compareTo(OrderConstant.OrderStatusEnum.CANCLED.getCode()) == 0){
                    // 解锁并更新工作单状态
                    unlockStockDB(skuId, wareId, num, detailToId);
                    // 解锁成功，手动回复
                }
            }else {
                // 远程调用失败，抛出异常，需要重新进行解锁
                throw new RuntimeException("远程服务调用失败");
            }
        }else {
            // 无需解锁
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

        List<SkuHasStockTo> skuHasStockTos = skuIds.stream().map((skuId) -> {
            SkuHasStockTo skuHasStockTo = new SkuHasStockTo();
            skuHasStockTo.setSkuId(skuId);
            // 每一个商品可能在不同仓库中，我们需要查的是总库存量
            // 总库存量=库存总量-锁定的库存(下单未发货)
            Long totalStock = baseMapper.getTotalStock(skuId);
            skuHasStockTo.setHasStock(totalStock != null && totalStock > 0);
            return skuHasStockTo;
        }).collect(Collectors.toList());

        return skuHasStockTos;
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
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
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




}
