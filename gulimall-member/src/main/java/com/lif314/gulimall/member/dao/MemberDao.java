package com.lif314.gulimall.member.dao;

import com.lif314.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lif314.gulimall.member.entity.MemberLevelEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会员
 *
 * @author lif314
 * @email lifer314@163.com
 * @date 2022-02-07 22:22:41
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {

    MemberLevelEntity getDefaultMemberLevel();

    MemberEntity selectByNameOrPhone(@Param("loginacct") String loginacct);
}
