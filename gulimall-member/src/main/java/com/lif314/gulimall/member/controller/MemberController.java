package com.lif314.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.lif314.common.exception.BizCodeEnum;
import com.lif314.gulimall.member.exception.PhoneExistException;
import com.lif314.gulimall.member.exception.UsernameExistException;
import com.lif314.gulimall.member.feign.CouponFeignService;
import com.lif314.gulimall.member.vo.MemberLoginVo;
import com.lif314.gulimall.member.vo.MemberRegisterVo;
import com.lif314.gulimall.member.vo.SocialUserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.lif314.gulimall.member.entity.MemberEntity;
import com.lif314.gulimall.member.service.MemberService;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.R;



/**
 * 会员
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:22:41
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    // 远程调用测试
    @Autowired
    CouponFeignService couponFeignService;

    @RequestMapping("/coupons")
    public R test(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");
        R membercoupons = couponFeignService.membercoupons();
        return R.ok().put("member", memberEntity).put("coupons", membercoupons.get("coupons"));
    }

    /***
     * 社交账号登录
     */
    @PostMapping("/oauth2/login")
    public R oauthlogin(@RequestBody SocialUserVo socialUserVo){
        MemberEntity entity = memberService.oauthLogin(socialUserVo);
        if(entity != null){
            // TODO 登录成功处理, 返回用户信息
            return R.ok().put("data", entity);
        }else{
            return R.error(BizCodeEnum.USER_LOGIN_EXCEPTION.getCode(), BizCodeEnum.USER_LOGIN_EXCEPTION.getMsg());
        }
    }


    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo){
        MemberEntity memberEntity = memberService.login(vo);
        if(memberEntity != null)
        {
            // 登录成功，返回用户信息
            return R.ok().put("data", memberEntity);
        }else{
            return R.error(BizCodeEnum.USER_LOGIN_EXCEPTION.getCode(), BizCodeEnum.USER_LOGIN_EXCEPTION.getMsg());
        }
    }

    /**
     * 注册用户
     *
     * 接受json数据
     */
    @PostMapping("/register")
    public R register(@RequestBody MemberRegisterVo vo){
        try{
            MemberEntity member = memberService.register(vo);
            return R.ok().put("data", member);
        }catch (PhoneExistException e){
            // 处理异常
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        }catch (UsernameExistException e) {
            return R.error(BizCodeEnum.USER_EXIST_EXCEPTION.getCode(), BizCodeEnum.USER_EXIST_EXCEPTION.getMsg());
        }
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
