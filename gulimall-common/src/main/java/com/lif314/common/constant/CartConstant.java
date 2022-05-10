package com.lif314.common.constant;

/**
 * 购物车常量
 */
public class CartConstant {

    // 临时用户的cookie名
    public static final String TEMP_USER_COOKIE_NAME= "user-key";
    // cookie过期时间--user-key 以s为单位  一个月

    public static final int TEMP_USER_COOKIE_TIMEOUT= 60 * 60 * 24 * 30;

    // Redis购物车的前缀
    public static final String CART_PREFIX = "feihong:cart";
}
