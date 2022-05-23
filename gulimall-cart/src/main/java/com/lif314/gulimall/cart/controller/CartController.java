package com.lif314.gulimall.cart.controller;

import com.lif314.common.utils.R;
import com.lif314.gulimall.cart.interceptor.CartInterceptor;
import com.lif314.gulimall.cart.service.CartService;
import com.lif314.gulimall.cart.to.UserInfoTo;
import com.lif314.gulimall.cart.vo.Cart;
import com.lif314.gulimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RequestMapping("/cart")
@RestController
public class CartController {


    @Autowired
    CartService cartService;

    /**
     * 获取购物车 【登录 / 未登录】
     * 京东：浏览器中有一个cookie: useer-key--标识用户用户身份，一个越后过期
     *     如果第一次使用jd的购物车，都会给一个临时用户身份
     *     浏览器以后报仇呢，每次访问都会带上这个cookie
     *
     * 登录：session有用户信息
     * 未登录：按照cookie里面带来的user-key来做。
     * 第一次，如果没有临时用户，帮忙临时创建一个临时用户
     *
     * @param session 判断用户是否登录
     */
    // 获取购物车列表
    @GetMapping("/list")
    public R cartListPage(HttpSession session){
        // 使用拦截器判断是否处于登录状态
        // 并快速获取用户信息 ThreadLocal -- 同一个线程共享数据
        // 拦截器共享了一个ThreadLocal -- 线程上下文
         UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        System.out.println("originalUser"+userInfoTo);
        Cart cart = cartService.getCart();
        // 清理ThreadLocal中的数据
//        CartInterceptor.threadLocal.remove();
        return R.ok().put("data", cart);
    }


    /**
     * 处理加入购物车请求
     *
     * @param skuId 商品的id
     * @param num 商品的数量
     */
    @GetMapping("/addToCart/{skuId}/{num}")
    public R addToCart(@PathVariable("skuId") Long skuId,
                            @PathVariable("num") Integer num) throws ExecutionException, InterruptedException {

        CartItem cartItem = cartService.addToCart(skuId, num);
        return R.ok().put("data", cartItem);
    }


    /**
     *跳到成功页
     */
    @GetMapping("/addToCartSuccess")
    public R addToCartSuccess(@RequestParam("skuId") Long skuId){
        // 重定向到成功也买你，再次查询购物车中的数据
        CartItem item = cartService.getCartItemRedis(skuId);
        return R.ok().put("data", item);
    }


    // 选中购物项: http://cart.feihong.com/checkItem?skuId=" + skuId + "&check="+(check?1:0)
    @GetMapping("/checkItem")
    public R checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check){
        cartService.checkItem(skuId, check);
        // 重定向到购物车列表页
        return R.ok();
    }

    /**
     * 改变商品数量
     * "http://cart.feihong.com/countItem?skuId=" + skuId + "&num="+countNum;
     */
    @GetMapping("/countItem")  // 需要跳转的都是Get请求
    public R countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num){
        cartService.changeItemCount(skuId, num);
        // 修改成功后，跳转购物车页面
        return R.ok();
    }

    /**
     * 删除购物项
     *
     * location.href = "http://cart.feihong.com/deleteItem?skuId=" + deleteSkuId;
     */
    @GetMapping("/deleteItem")
    public R deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return R.ok();
    }


    /**
     * 获取当前用户选中的购物项
     */
    @GetMapping("/currentUserCartItems")
    public List<CartItem> getCurrentUserCartItems(){
       return cartService.getCurrentUserCartItems();
    }
}
