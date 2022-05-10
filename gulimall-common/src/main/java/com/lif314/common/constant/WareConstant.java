package com.lif314.common.constant;

public class WareConstant {

    public enum PurchaseStatusEnum{
        /**
         *           <el-option label="新建" :value="0"></el-option>
         *           <el-option label="已分配" :value="1"></el-option>
         *           <el-option label="已领取" :value="2"></el-option>
         *           <el-option label="已完成" :value="3"></el-option>
         *           <el-option label="有异常" :value="4"></el-option>
         */

        CREATED(0, "新建"),

        ASSSIGNED(1, "已分配"),

        RECEIVED(2, "已领取"),

        FINISH(3, "已完成"),

        HASERROR(4, "有异常");

        private int status;
        private String msg;

        PurchaseStatusEnum(int status, String msg){
            this.status = status;
            this.msg = msg;
        }

        public int getStatus() {
            return status;
        }

        public String getMsg() {
            return msg;
        }
    }


    /**
     * 采购需求状态详情 -- 与采购单的关系状态
     */
    public enum PurchaseDetailStatusEnum{

        CREATED(0, "新建"),

        ASSSIGNED(1, "已分配"),

        BUYING(2, "正在采购"),

        FINISH(3, "已分配"),

        FAILURE(4, "采购失败");

        private int status;
        private String msg;

        PurchaseDetailStatusEnum(int status, String msg){
            this.status = status;
            this.msg = msg;
        }

        public int getStatus() {
            return status;
        }

        public String getMsg() {
            return msg;
        }
    }


    /**
     * 采购需求状态详情 -- 与采购单的关系状态
     */
    public enum WareStockLockStatus{

        LOCKED(1, "已锁定"),

        UNLOCKED(2, "已解锁"),

        REDUCE(3, "扣减");

        private Integer status;
        private String msg;

        WareStockLockStatus(int status, String msg){
            this.status = status;
            this.msg = msg;
        }

        public int getStatus() {
            return status;
        }

        public String getMsg() {
            return msg;
        }
    }

}
