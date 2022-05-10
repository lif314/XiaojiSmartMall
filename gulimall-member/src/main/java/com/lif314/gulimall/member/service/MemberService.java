package com.lif314.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lif314.common.utils.PageUtils;
import com.lif314.gulimall.member.entity.MemberEntity;
import com.lif314.gulimall.member.exception.PhoneExistException;
import com.lif314.gulimall.member.exception.UsernameExistException;
import com.lif314.gulimall.member.vo.MemberLoginVo;
import com.lif314.gulimall.member.vo.MemberRegisterVo;
import com.lif314.gulimall.member.vo.SocialUserVo;

import java.net.UnknownServiceException;
import java.util.Map;

/**
 * 会员
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:22:41
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    MemberEntity register(MemberRegisterVo vo);


    void checkUserNameUnique(String userName) throws UsernameExistException;

    void checkPhoneUnique(String phone) throws PhoneExistException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity oauthLogin(SocialUserVo socialUserVo);
}

