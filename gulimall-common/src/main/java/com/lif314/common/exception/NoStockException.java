package com.lif314.common.exception;


public class NoStockException  extends RuntimeException{

    private Long skuId;
    public NoStockException(Long skuId ){
        super("商品Id:"+ skuId +"没有足够的库存");
    }

    public NoStockException(String msg){
        super(msg +", 远程调用失败，可能是没有足够的库存|服务没有上线");
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Long getSkuId() {
        return skuId;
    }
}
