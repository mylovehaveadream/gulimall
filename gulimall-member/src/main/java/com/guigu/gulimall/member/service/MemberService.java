package com.guigu.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.guigu.common.utils.PageUtils;
import com.guigu.gulimall.member.entity.MemberEntity;
import com.guigu.gulimall.member.exception.PhoneExitException;
import com.guigu.gulimall.member.exception.UsernameExitException;
import com.guigu.gulimall.member.vo.MemberLoginVo;
import com.guigu.gulimall.member.vo.MemberRegistVo;
import com.guigu.gulimall.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author mylovehaveadream
 * @email mylovehaveadream@gmail.com
 * @date 2020-08-26 18:59:25
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkUsernameUnique(String username) throws UsernameExitException;

    void checkPhoneUnique(String phone) throws PhoneExitException;

    MemberEntity login(MemberLoginVo vo);

    MemberEntity login(SocialUser socialUser) throws Exception;

}

