package com.lif314.gulimall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.constant.OrderConstant;
import com.lif314.common.exception.NoStockException;
import com.lif314.common.to.MemberRespTo;
import com.lif314.common.to.mq.SeckillOrderTo;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;
import com.lif314.common.utils.R;
import com.lif314.gulimall.order.dao.OrderDao;
import com.lif314.gulimall.order.entity.OrderEntity;
import com.lif314.gulimall.order.entity.OrderItemEntity;
import com.lif314.gulimall.order.entity.PaymentInfoEntity;
import com.lif314.gulimall.order.feign.CartFeignService;
import com.lif314.gulimall.order.feign.MemberFeignService;
import com.lif314.gulimall.order.feign.ProductFeignService;
import com.lif314.gulimall.order.feign.WareFeignService;
import com.lif314.gulimall.order.intercepter.LoginUserInterceptor;
import com.lif314.gulimall.order.service.OrderItemService;
import com.lif314.gulimall.order.service.OrderService;
import com.lif314.gulimall.order.service.PaymentInfoService;
import com.lif314.gulimall.order.to.*;
import com.lif314.gulimall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;

import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    /**
     * 使用ThreadLocal共享页面订单数据，这样可以避免多次传参
     */
    private static final ThreadLocal<OrderSubmitVo> submitOrderThreadLocal = new ThreadLocal<>();

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    PaymentInfoService paymentInfoService;


    /**
     * 分页查询：获取游湖已经支付成功的订单
     */
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        // 获取当前用户信息
        MemberRespTo memberRespTo = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(new Query<OrderEntity>().getPage(params), new QueryWrapper<OrderEntity>().eq("member_id", memberRespTo.getId()).orderByDesc("id"));


        // 获取订单关联的订单项信息
        List<OrderEntity> collect = page.getRecords().stream().map((order) -> {
            List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(order_sn);
            return order;
        }).collect(Collectors.toList());

        IPage<OrderEntity> orderEntityIPage = page.setRecords(collect);

        return new PageUtils(orderEntityIPage);
    }

    /**
     * 处理支付宝异步 支付结果
     */
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        // 1、保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());

        paymentInfoService.save(infoEntity);

        // 2、修改订单状态
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            // 支付成功
            String orderSn = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(orderSn, OrderConstant.OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    // TODO 在支付宝验签后才能更新订单的状态
    @Override
    public void updateOrderStatus(String orderSn) {
        this.baseMapper.updateOrderStatus(orderSn, OrderConstant.OrderStatusEnum.PAYED.getCode());
    }

    /**
     * 创建秒杀订单
     */
    @Override
    public void cereateSeckillOrder(SeckillOrderTo seckillOrder) {
        // TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrder.getOrderSn());
        orderEntity.setMemberId(seckillOrder.getMemberId());

        orderEntity.setStatus(OrderConstant.OrderStatusEnum.CREATE_NEW.getCode());

        BigDecimal totalPrice = seckillOrder.getSeckillPrice().multiply(new BigDecimal(seckillOrder.getNum().toString()));
        orderEntity.setPayAmount(totalPrice);

        // TODO 收获信息
        this.save(orderEntity);


        // TODO 保存订单项信息  远程调用进行设置
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderSn(seckillOrder.getOrderSn());
        orderItem.setSkuId(seckillOrder.getSkuId());
        orderItem.setRealAmount(totalPrice);
        orderItem.setSkuQuantity(seckillOrder.getNum());

        orderItemService.save(orderItem);
    }


    /**
     * 定时关闭订单
     */
    @Override
    public void closeOrder(OrderEntity orderEntity) {
        Long id = orderEntity.getId();
        // 获取数据库中订单的最新状态
        OrderEntity newOrder = this.getById(id);

        // 订单状态 --- 待付款才进行关单
        if (newOrder != null && newOrder.getStatus().compareTo(OrderConstant.OrderStatusEnum.CREATE_NEW.getCode()) == 0) {
            // 关闭订单：状态改为已经取消
            OrderEntity order = new OrderEntity();
            order.setId(newOrder.getId());
            order.setStatus(OrderConstant.OrderStatusEnum.CANCLED.getCode());
            this.updateById(order);

            // 订单解锁成功，给库存发送解锁库存消息
            // 该交换机会将消息发送给库存
            try {
                // TODO 保证消息一定发送出去， 每一个消息做日志记录(给数据库保存消息详细信息，定期扫描进行恢复)
                // 定期扫描数据库将失败的消息再发送一遍
//                String s = JSON.toJSONString(newOrder);
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", newOrder);
            } catch (Exception e) {
                // TODO 引入重试机制：将没法成功的消息重试几次进行发送
            }

        }
    }

    @Override
    public PayVo getOrderByOrderSn(String orderSn) {
        OrderEntity orderEntity = this.baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
//        System.out.println("orderEntity:" + orderEntity.toString());
        PayVo payVo = new PayVo();
        // 支付宝只能识别两位小数: 有小数就向上取值
        BigDecimal bigDecimal = orderEntity.getPayAmount().setScale(2, RoundingMode.UP);
        payVo.setTotal_amount(bigDecimal.toString());
        // 交易号
        payVo.setOut_trade_no(orderEntity.getOrderSn());
        // 交易标题
//        List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
//        OrderItemEntity itemEntity = order_sn.get(0);
//        payVo.setSubject(itemEntity.getSkuName());
        payVo.setSubject("小济智家商城购物");
        // 交易备注
        payVo.setBody("小济智家商城购物");
        return payVo;
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(new Query<OrderEntity>().getPage(params), new QueryWrapper<OrderEntity>());

        return new PageUtils(page);
    }

    /**
     * 订单确认页数据
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        MemberRespTo memberRespTo = LoginUserInterceptor.loginUser.get();
//        System.out.println("ordermem" + memberRespTo);
        // 主线程  RequestContextHolder -- 使用ThreadLocal共享数据
        // 获取之前的请求信息，每一个请求都应该共享数据
        RequestAttributes mainThreadRequest = RequestContextHolder.getRequestAttributes();

        // 使用异步编排
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            // 子线程
            RequestContextHolder.setRequestAttributes(mainThreadRequest);
            // 1、远程查询收获地址信息
            List<MemberAddressTo> memberAddress = memberFeignService.getMemberAddress(memberRespTo.getId());
            orderConfirmVo.setAddress(memberAddress);
        }, executor);


        CompletableFuture<Void> getItemsFuture = CompletableFuture.runAsync(() -> {
            // 子线程
            RequestContextHolder.setRequestAttributes(mainThreadRequest);
            // 2、远程查询购物车中的购物项
            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            orderConfirmVo.setItems(currentUserCartItems);
        }, executor);

        // 3、查询用户积分
        Integer integration = memberRespTo.getIntegration();
        orderConfirmVo.setInteration(integration);

        // 4、其它属性自动计算

        CompletableFuture.allOf(getItemsFuture, getAddressFuture).get();

        // TODO 5、防止重复提交下单请求 -- 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        // 防重令牌需要分发给服务器和页面，然后页面发送请求时带着令牌进行比对
        orderConfirmVo.setOrderToken(token);  // 页面
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespTo.getId(), token, 30, TimeUnit.MINUTES);  // Redis

        return orderConfirmVo;
    }

    /**
     * 去创建订单，验令牌，验价格，锁库存。。。。
     */
    @Transactional
    @Override
    public SubmitOrderRespVo submitOrder(OrderSubmitVo submitVo) {
        // 共享再threadLocal中，创建订单数据，共享在线程中
        submitOrderThreadLocal.set(submitVo);

        // 从拦截器中获取当前登录的用户
        MemberRespTo memberRespTo = LoginUserInterceptor.loginUser.get();

        // 返回对象
        SubmitOrderRespVo respVo = new SubmitOrderRespVo();
        // 默认下单成功
        respVo.setCode(0);


        // 1. 验证令牌【对比和删除必须保证原子性】
        String orderToken = submitVo.getOrderToken();

        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        // 返回值，0 失败   1成功
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespTo.getId()), orderToken);
//        System.out.println("vali_res" + result);
        if (result == 0L) {
            // 验证失败-- 订单信息过期
            respVo.setCode(1);
            return respVo;
        } else {
            // 验证成功
            // 1、创建订单
            OrderCreateTo orderCreateTo = createOrder();
            // 2、验价
            BigDecimal payAmount = orderCreateTo.getOrder().getPayAmount();
            BigDecimal payPrice = submitVo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {

                // 3、对比成功--保存订单到数据库中
                saveOrder(orderCreateTo);

                // 4、锁定库存 -- 只要有异常回滚订单数据
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(orderCreateTo.getOrder().getOrderSn());
                List<SkuItemLockTo> skuItemLockTos = orderCreateTo.getOrderItems().stream().map((item) -> {
                    SkuItemLockTo skuItemLockTo = new SkuItemLockTo();
                    skuItemLockTo.setSkuId(item.getSkuId());
                    skuItemLockTo.setCount(item.getSkuQuantity());
                    return skuItemLockTo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(skuItemLockTos);

                // TODO 远程锁库存
                // 采用重传机制，重新发送三次请求，超过三次则默认失败，抛出异常
                R r = wareFeignService.orderLockStock(wareSkuLockVo);

                System.out.println("远程锁库存操作=========="+ r.getCode());
                if (r.getCode() == 0) {
                    // 请求成功，终止请求 直接return，不会继续循环
                    // 库存锁定成功
                    respVo.setCode(0);
                    respVo.setOrder(orderCreateTo.getOrder());

//                    String s = JSON.toJSONString(orderCreateTo.getOrder());
                    // TODO 订单创建成功发送消息给MQ, 直接发送order
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", orderCreateTo.getOrder());

                    return respVo;
                } else {
                    // 重传失败，远程调用失败：可能有多种原因：服务还没有上线 | 库存不足等
                    // 失败 -- 库存锁定失败，可能有多种原因：服务还没有上线 | 库存不足等
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
//                    respVo.setCode(3);
//                    return respVo;
                    // TODO 远程扣除积分
//                    respVo.setCode(3);
//                    return respVo;
                }
            } else {
                // 验价失败，商品价格信息发生改变
                respVo.setCode(2);
                return respVo;
            }
        }
    }

    /**
     * 根据订单号查询订单信息
     */
    @Override
    public OrderEntity getOrderStatusByOrderSn(String orderSn) {
        return this.baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }


    /**
     * 保存订单数据
     */
    private void saveOrder(OrderCreateTo order) {
        // 保存订单数据
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        // 保存订单项数据
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    /**
     * 创建订单
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();

        // 1、生成订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity order = buildOrder(orderSn);
        createTo.setOrder(order);

        // 2、获取所有订单项数据
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        createTo.setOrderItems(orderItemEntities);

        // 3、验价
        assert orderItemEntities != null;
        computePrice(order, orderItemEntities);

        return createTo;
    }

    // 计算价格相关
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        // 1.订单总额、促销总金额、优惠券总金额、积分优惠总金额
        BigDecimal total = new BigDecimal(0);
        BigDecimal coupon = new BigDecimal(0);
        BigDecimal promotion = new BigDecimal(0);
        BigDecimal integration = new BigDecimal(0);
        // 2.积分、成长值
        Integer giftIntegration = 0;
        Integer giftGrowth = 0;
        for (OrderItemEntity itemEntity : orderItemEntities) {
            total = total.add(itemEntity.getRealAmount());// 订单总额
            coupon = coupon.add(itemEntity.getCouponAmount());// 促销总金额
            promotion = promotion.add(itemEntity.getPromotionAmount());// 优惠券总金额
            integration = integration.add(itemEntity.getIntegrationAmount());// 积分优惠总金额
            giftIntegration = giftIntegration + itemEntity.getGiftIntegration();// 积分
            giftGrowth = giftGrowth + itemEntity.getGiftGrowth();// 成长值
        }
        orderEntity.setTotalAmount(total);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(integration);

        // 设置积分等信息
        orderEntity.setIntegration(giftIntegration);// 总积分信息
        orderEntity.setGrowth(giftGrowth);// 总成长值

        // 3.应付总额
//        orderEntity.setPayAmount(orderEntity.getTotalAmount().add(orderEntity.getFreightAmount()));// 订单总额 +　运费
        orderEntity.setPayAmount(orderEntity.getTotalAmount());// 订单总额 +　运费
    }

    /**
     * 构建订单项列表
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        // 最后确定每一个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> collect = currentUserCartItems.stream().map((item) -> buildOrderItem(item, orderSn)).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * 构建订单项
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem, String orderSn) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        // 1、订单信息：订单号
        orderItemEntity.setOrderSn(orderSn);
        // 2、商品SPU信息
        Long skuId = cartItem.getSkuId();
        R spuInfo = productFeignService.getSpuInfoBySkuId(skuId);
        if (spuInfo.getCode() == 0) {
            // 获取成功
            Object data = spuInfo.get("data");
            String s = JSON.toJSONString(data);
            SpuInfoTo spuInfoTo = JSON.parseObject(s, SpuInfoTo.class);
            orderItemEntity.setSpuId(spuInfoTo.getId());
            orderItemEntity.setSpuName(spuInfoTo.getSpuName());
            orderItemEntity.setSpuBrand(spuInfoTo.getBrandId().toString());
            orderItemEntity.setCategoryId(spuInfoTo.getCatalogId());
        }

        // 3、商品SKU信息
        orderItemEntity.setSkuId(cartItem.getSkuId());
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImage());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttr);
        orderItemEntity.setSkuQuantity(cartItem.getCount());

        // TODO 4、优惠信息

        // 5、积分信息
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        // TODO 6、订单项的价格信息
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        // 当前订单项的实际价格
        BigDecimal originPrice = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        BigDecimal subtract = originPrice.subtract(orderItemEntity.getPromotionAmount()).subtract(orderItemEntity.getCouponAmount()).subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);

        return orderItemEntity;
    }

    /**
     * 构建订单
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberRespTo memberRespTo = LoginUserInterceptor.loginUser.get();
        OrderEntity order = new OrderEntity();
        order.setOrderSn(orderSn);
        // 设置会员信息
        order.setMemberId(memberRespTo.getId());
        OrderSubmitVo orderSubmitVo = submitOrderThreadLocal.get();
        // 2、远程获取收获信息
        R fare = wareFeignService.getFare(orderSubmitVo.getAddrId());
        if (fare.getCode() == 0) {
            // 获取成功
            Object data = fare.get("data");
            String s = JSON.toJSONString(data);
            FareTo fareTo = JSON.parseObject(s, FareTo.class);
            MemberAddressTo address = fareTo.getAddress();
            // 运费信息
            order.setFreightAmount(fareTo.getFare());
            // 收获地址信息
            order.setReceiverCity(address.getCity());
            order.setReceiverDetailAddress(address.getDetailAddress());
            order.setReceiverName(address.getName());
            order.setReceiverPhone(address.getPhone());
            order.setReceiverPostCode(address.getPostCode());
            order.setReceiverProvince(address.getProvince());
            order.setReceiverRegion(address.getRegion());
        }

        // 设置订单相关状态信息
        order.setStatus(OrderConstant.OrderStatusEnum.CREATE_NEW.getCode());
        order.setDeleteStatus(0); // 0代表未删除
        return order;
    }

}
