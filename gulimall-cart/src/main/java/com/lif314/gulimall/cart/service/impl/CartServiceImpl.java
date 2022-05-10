package com.lif314.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lif314.common.constant.AuthServerConstant;
import com.lif314.common.constant.CartConstant;
import com.lif314.common.utils.R;
import com.lif314.gulimall.cart.feign.ProductFeignService;
import com.lif314.gulimall.cart.interceptor.CartInterceptor;
import com.lif314.gulimall.cart.service.CartService;
import com.lif314.gulimall.cart.to.SkuInfoVo;
import com.lif314.gulimall.cart.to.UserInfoTo;
import com.lif314.gulimall.cart.vo.Cart;
import com.lif314.gulimall.cart.vo.CartItem;
import com.lif314.gulimall.cart.vo.CartItemPriceMapVo;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    // 线程池
    @Autowired
    ThreadPoolExecutor executor;

    /**
     * 添加购物车 -- 添加在Redis中
     *
     * @param skuId 商品id
     * @param num   商品数量
     * @return 购物项
     */
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        // 先判断Redis中是否已经存在商品信息
        String res = (String) cartOps.get(skuId.toString());
        if(StringUtils.isEmpty(res)){
            // 2 添加新商品在购物车
            CartItem cartItem = new CartItem();
            // 异步编排
            CompletableFuture<Void> getSkuInfo = CompletableFuture.runAsync(() -> {
                // 远程获取商品信息
                R r = productFeignService.getSkuInfo(skuId);
                if (r.getCode() == 0) {
                    Object data = r.get("skuInfo");
                    SkuInfoVo skuInfoVo = JSON.parseObject(JSON.toJSONString(data), SkuInfoVo.class);
                    // 商品加入购物项
                    cartItem.setCheck(true);
                    cartItem.setCount(num);
                    cartItem.setImage(skuInfoVo.getSkuDefaultImg());
                    cartItem.setTitle(skuInfoVo.getSkuTitle());
                    cartItem.setSkuId(skuId);
                    cartItem.setPrice(skuInfoVo.getPrice());
                }
            }, executor);

            CompletableFuture<Void> getSaleAttrs = CompletableFuture.runAsync(() -> {
                // 远程查询属性信息
                // TODO 多个远程服务查询，可以放在线程池中
                List<String> saleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(saleAttrValues);
            }, executor);

            // 需要等异步处理结束后才能获取数据
            CompletableFuture.allOf(getSkuInfo, getSaleAttrs).get();

            // 保存在Redis中
            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), s);
            return cartItem;
        }else{
            // 更新商品的数量
            CartItem item = JSON.parseObject(res, CartItem.class);
            String s = JSON.toJSONString(item);
            item.setCount(item.getCount() + num);
            cartOps.put(item.getSkuId().toString(), s);
            return item;
        }
    }

    @Override
    public CartItem getCartItemRedis(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString());
        return JSON.parseObject(res, CartItem.class);
    }

    /**
     * 获取购物车所有数据
     *
     * TODO 由于用户信息放在LocalThread，随时随地都能获取，所以没有参数用户ID
     */
    @Override
    public Cart getCart() {
        // 获取用户数据
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        Cart cart = new Cart();
        // 判断用户是否登录
        if(userInfoTo.getUserId() != null){
            // 登录: 需要合并临时购物车和正式购物车

            // 获取正式的购物车
            String userIdKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItemsByUserId = getCartByKey(userIdKey);

            // 获取临时购物车
            String userKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCarts = getCartByKey(userKey);
            Map<Long, CartItem> itemMaps = null;
            if(tempCarts != null && tempCarts.size() > 0){
                // 临时购物车有数据，需要合并
                // 将正式购物车中的数据映射为 <skuId, count>
                // TODO 购物车合并
                // 将正式购物车中的数据映射为Map,Key为skuId, 值为对象，然后通过临时购物车中的skuId
                // 判断是否已经在购物车中：如果在，则将正式购物车中的数量加上；如果不在，则添加到正式购物车中
                itemMaps = new HashMap<>();
                for (CartItem item : cartItemsByUserId) {
                    itemMaps.put(item.getSkuId(), item);
                }
                for (CartItem tempCart : tempCarts) {
                    CartItem cartItem = itemMaps.get(tempCart.getSkuId());
                    if(cartItem != null){
                        // 正式购物车中已经存在该商品
                        cartItem.setCount(cartItem.getCount() + tempCart.getCount());
                        // 删除原有数据---> 不需要删除,Redis会按照key进行更新
//                        itemMaps.remove(tempCart.getSkuId());
                        // 保存更新的数据
                        itemMaps.put(cartItem.getSkuId(), cartItem);
                    }else{
                        // 直接存入正式购物车中
                        itemMaps.put(tempCart.getSkuId(), tempCart);
                    }
                }
            }

            if(itemMaps != null){
                // 经过合并

                List<CartItem> values = itemMaps.values().stream().map((item)->{
                    CartItem cartItem = new CartItem();
                    BeanUtils.copyProperties(item, cartItem);
                    return cartItem;
                }).collect(Collectors.toList());
                cart.setItems(values);
            }else{
                //没有合并过
                cart.setItems(cartItemsByUserId);
            }
            // 清除临时购物车 -- 这个应该可以删除hash中的数据
            redisTemplate.delete(userKey);
        }else {
            // 没有登录
            String userKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
            cart.setItems(getCartByKey(userKey));
        }

        return cart;
    }

    /**
     * 选中购物项
     */
    @Override
    public void checkItem(Long skuId, Integer check) {
        CartItem cartItem = getCartItemRedis(skuId);
        cartItem.setCheck(check == 1);
        String s = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        // TODO 同一个key，redis会进行更新吗？ 是的，直接更新，不用删除
        cartOps.put(skuId.toString(), s);
    }

    /**
     * 改变商品的数量
     */
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItemRedis(skuId);
        cartItem.setCount(num);
        String s = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), s);
    }

    /**
     * 删除购物项
     */
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    /**
     * 获取当前用户选中的购物项
     * @return
     */
    @Override
    public List<CartItem> getCurrentUserCartItems() {
        // 使用拦截器获取用户身份信息
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if(userInfoTo ==  null){
            // 还没有登录
            return null;
        }else{
            String userIdKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
            // 获取所有购物项
            List<CartItem> cartByKey = getCartByKey(userIdKey);
            List<CartItem> collect = cartByKey.stream().filter(CartItem::getCheck).collect(Collectors.toList());
            // 远程查询：价格是Redis中的价格，可能已经改变了，所以需要更新价格
            List<Long> cartItemIds = collect.stream().map(CartItem::getSkuId).collect(Collectors.toList());
            R r = productFeignService.getCartItemNewPrices(cartItemIds);
            if(r.getCode() == 0){
                Object data = r.get("data");
                String s = JSON.toJSONString(data);
                CartItemPriceMapVo cartItemPriceMapVo = JSON.parseObject(s, CartItemPriceMapVo.class);
                Map<Long, BigDecimal> itemNewPrices = cartItemPriceMapVo.getItemNewPrice();
                // 不能直接传输Map类型的数据，Feign会将其转化为JSON，导致无法解析成功
                return collect.stream().map((cartItem) -> {
                    BigDecimal price = itemNewPrices.get(cartItem.getSkuId());
                    cartItem.setPrice(price);
                    return cartItem;
                }).collect(Collectors.toList());
            }else{
                return null;
            }

        }
    }

    /**
     * 根据key获取购物车中的数据
     */
    private List<CartItem> getCartByKey(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        List<CartItem> cartItems = new ArrayList<>();
        if(values != null && values.size() > 0){
            cartItems = values.stream().map((obj) -> {
                String body = JSONObject.toJSONString(obj);
                Object parse1 = JSON.parse(body);
                String s = parse1.toString();
                CartItem cartItem = JSON.parseObject(s, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
        }
        return cartItems;
    }


    // 封装对Redis的操作
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            // 已经登陆了
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
        } else {
            // 临时购物车
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
        }

        // 将商品信息存在Redis中
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        return hashOps;
    }
}
