package com.guigu.gulimall.member.dao;

import com.guigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author mylovehaveadream
 * @email mylovehaveadream@gmail.com
 * @date 2020-08-26 18:59:25
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
