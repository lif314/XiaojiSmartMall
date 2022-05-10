package com.lif314.common.constant;

/**
 * 商品系统相关的常量
 */
public class ProductConstant {

    public enum AttrEnum{
        // 基本属性-1 销售属性-0
        ATTR_TYPE_BASE(1, "基本属性"),
        ATTR_TYPE_SALE(0, "销售属性");

        private int code;
        private String msg;

        AttrEnum(int code, String msg){
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }

    public enum ProductStatusEnum{
        SPU_NEW(0, "新建"),
        SPU_UP(1, "上架"),
        SPU_DOWN(2,"下架");

        private int code;
        private String msg;

        ProductStatusEnum(int code, String msg){
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }

}
