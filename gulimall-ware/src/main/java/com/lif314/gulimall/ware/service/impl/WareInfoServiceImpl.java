package com.lif314.gulimall.ware.service.impl;

import com.alibaba.fastjson.JSON;
import com.lif314.common.utils.R;
import com.lif314.gulimall.ware.feign.MemberFeignService;
import com.lif314.gulimall.ware.vo.FareVo;
import com.lif314.gulimall.ware.vo.MemberAddressVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.ware.dao.WareInfoDao;
import com.lif314.gulimall.ware.entity.WareInfoEntity;
import com.lif314.gulimall.ware.service.WareInfoService;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        // 添加模糊查询
        String key = (String) params.get("key");
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(key)){
            wrapper.eq("id", key)
                    .or().like("name", key)
                    .or().like("address", key)
                    .or().like("areacode", key);
        }

        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 远程获取运费信息
     */
    @Override
    public FareVo getFare(Long addrId) {
        FareVo fareVo = new FareVo();
        // 从gulimall-member中获取收货地址详细信息
        // TODO 整合第三方服务计算运费
        R info = memberFeignService.info(addrId);
        if (info.getCode() == 0) {
            // 获取成功
            Object memberReceiveAddress = info.get("memberReceiveAddress");
            if (memberReceiveAddress != null) {
                String s = JSON.toJSONString(memberReceiveAddress);
                MemberAddressVo memberAddressVo = JSON.parseObject(s, MemberAddressVo.class);
                // 获取收货人地址信息
                fareVo.setAddress(memberAddressVo);
                // TODO 模拟运费信息，使用电话号码
//                String phone = memberAddressVo.getPhone();
//                String substring = phone.substring(phone.length() - 2);
                // 运费价格
                fareVo.setFare(new BigDecimal('0'));

                return fareVo;
            }
        }
        return null;
    }

}
