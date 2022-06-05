package com.lif314.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.lif314.common.constant.AuthServerConstant;
import com.lif314.common.to.MemberRespTo;
import com.lif314.gulimall.member.entity.MemberLevelEntity;
import com.lif314.gulimall.member.exception.PhoneExistException;
import com.lif314.gulimall.member.exception.UsernameExistException;
import com.lif314.gulimall.member.vo.MemberLoginVo;
import com.lif314.gulimall.member.vo.MemberRegisterVo;
import com.lif314.gulimall.member.vo.SocialUserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lif314.common.utils.PageUtils;
import com.lif314.common.utils.Query;

import com.lif314.gulimall.member.dao.MemberDao;
import com.lif314.gulimall.member.entity.MemberEntity;
import com.lif314.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberDao memberDao;
    @Autowired
    StringRedisTemplate redisTemplate;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 注册用户信息
     */
    @Override
    public MemberEntity register(MemberRegisterVo vo) {
        MemberEntity memberEntity = new MemberEntity();
        // 先要判断用户名和手机号是否已经存在
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());

        // 获取默认等级id的ID
        MemberLevelEntity memberLevel = getDefaultMemberLevel();
        memberEntity.setLevelId(memberLevel.getId());

        memberEntity.setUsername(vo.getUserName());
        // 默认昵称为用户名
        memberEntity.setNickname(vo.getUserName());
        memberEntity.setMobile(vo.getPhone());

        // TODO 密码加密存储  MD5--盐值(拼接随机值)加密
        // 验证：密码加上盐值加密后对比
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        // 匹配密码
        // boolean matches = passwordEncoder.matches(vo.getPassword(), encode);
        memberEntity.setPassword(encode);
        // 保存数据
        this.baseMapper.insert(memberEntity);
        // 返回用户信息
        return memberEntity;
    }

    @Override
    public void checkUserNameUnique(String userName) throws UsernameExistException {
        Long userCount = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if(userCount > 0L){
            throw new UsernameExistException();
        }
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        Long mobile = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(mobile > 0L){
            throw new PhoneExistException();
        }
    }

    /**
     * 用户登录
     */
    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();
        // 在数据库中查询 -- 用户名或者手机号
        MemberEntity memberEntity = this.baseMapper.selectByNameOrPhone(loginacct);
        if(memberEntity == null){
            return null;
        }
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if(passwordEncoder.matches(password, memberEntity.getPassword())){
            // 登录成功
            return memberEntity;
        }else {
            // 账号或密码错误
//            throw new RuntimeException("密码错误");
            return null;
        }
    }

    /**
     * gitee社交登录
     */
    @Override
    public MemberEntity oauthLogin(SocialUserVo socialUserVo) {
        // 登录和注册合并逻辑
        // 社交账号唯一id
        Long socialUid = socialUserVo.getSocialUid();
        String socialType = socialUserVo.getSocialType();
        // 判断该用户是否已经在网站登录过
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", socialUid).eq("social_type", socialType));
        if(memberEntity != null){
            // 已经登录过, 返回该用户的信息
            return memberEntity;
        }else{
            // 没有登录过，需要新的注册
            MemberEntity newMember = new MemberEntity();
            // 为了多平台登录兼容性，随机生成用户昵称
            // 使用社交类型加Hi作为昵称
            newMember.setNickname("Hi,"+socialType + "User");
            newMember.setSocialUid(socialUid);
            newMember.setLevelId(1L);  // 默认普通会员
            newMember.setSocialType(socialType);
            newMember.setCreateTime(new Date());
            memberDao.insert(newMember);
            // 返回注册的对象
            return newMember;
        }
    }

    @Override
    public MemberRespTo getUserInfo(String token) {
        String key = AuthServerConstant.LOGIN_USER + token;
        System.out.println("key:"+key);
        String s = redisTemplate.opsForValue().get(key);

        MemberRespTo userInfoVO = JSON.parseObject(s, new TypeReference<MemberRespTo>() {
        });
        System.out.println("userinfo"+userInfoVO);
        return userInfoVO;
    }

    private MemberLevelEntity getDefaultMemberLevel() {
        return this.baseMapper.getDefaultMemberLevel();
    }

}
