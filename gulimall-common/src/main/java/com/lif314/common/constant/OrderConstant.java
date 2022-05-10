package com.lif314.common.constant;

public class OrderConstant {
    // 使用令牌机制解决订单提交的幂等性问题
    public static final String USER_ORDER_TOKEN_PREFIX = "order:token";

    public static final Integer autoConfirmDay = 7;

    /**
     * 订单状态枚举
     */
    public enum OrderStatusEnum {
        CREATE_NEW(0,"待付款"),
        PAYED(1,"已付款"),
        SENDED(2,"已发货"),
        RECIEVED(3,"已完成"),
        CANCLED(4,"已取消"),
        SERVICING(5,"售后中"),
        SERVICED(6,"售后完成");
        private Integer code;
        private String msg;

        OrderStatusEnum(Integer code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public Integer getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }

}
