package com.lif314.gulimall.cart.controller;

import com.lif314.gulimall.cart.service.CartService;
import com.lif314.gulimall.cart.vo.Cart;
import com.lif314.gulimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.ExecutionException;


@Controller
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
    @GetMapping("/cart.html")
    public String cartListPage(HttpSession session, Model model){
        // 使用拦截器判断是否处于登录状态
        // 并快速获取用户信息 ThreadLocal -- 同一个线程共享数据
        // 拦截器共享了一个ThreadLocal -- 线程上下文
        // UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);
        // 清理ThreadLocal中的数据
//        CartInterceptor.threadLocal.remove();
        return "cartList";
    }


    /**
     * 处理加入购物车请求
     *
     * @param skuId 商品的id
     * @param num 商品的数量
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {

        CartItem cartItem = cartService.addToCart(skuId, num);
        // 重定向到新的页面，查询数据进行展示
        redirectAttributes.addAttribute("skuId", skuId);
        /**
         * redirectAttributes.addAttribute：将数据放在URL中，作为路径变量
         * redirectAttributes.addFlashAttribute: 将数据放在Session中，
         *      可以在也页面取出，但只能取一次
         */
        // 添加成功，会带success页面
        // 这种方法每次刷新都会更改商品的数量
        // 可以使用重定向来解决
        return "redirect:http://cart.feihong.com/addToCartSuccess.html";
    }


    /**
     *跳到成功页
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccess(@RequestParam("skuId") Long skuId, Model model){
        // 重定向到成功也买你，再次查询购物车中的数据
        CartItem item = cartService.getCartItemRedis(skuId);
        model.addAttribute("item", item);
        return "success";
    }


    // 选中购物项: http://cart.feihong.com/checkItem?skuId=" + skuId + "&check="+(check?1:0)
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check){
        cartService.checkItem(skuId, check);
        // 重定向到购物车列表页
        return "redirect:http://cart.feihong.com/cart.html";
    }

    /**
     * 改变商品数量
     * "http://cart.feihong.com/countItem?skuId=" + skuId + "&num="+countNum;
     */
    @GetMapping("/countItem")  // 需要跳转的都是Get请求
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num){
        cartService.changeItemCount(skuId, num);
        // 修改成功后，跳转购物车页面
        return "redirect:http://cart.feihong.com/cart.html";
    }

    /**
     * 删除购物项
     *
     * location.href = "http://cart.feihong.com/deleteItem?skuId=" + deleteSkuId;
     */
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.feihong.com/cart.html";
    }


    /**
     * 获取当前用户选中的购物项
     */
    @ResponseBody  // 返回json数据
    @GetMapping("/currentUserCartItems")
    public List<CartItem> getCurrentUserCartItems(){
       return cartService.getCurrentUserCartItems();
    }



}
