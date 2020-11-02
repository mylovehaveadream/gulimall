package com.guigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.guigu.common.utils.HttpUtils;
import com.guigu.gulimall.member.dao.MemberLevelDao;
import com.guigu.gulimall.member.entity.MemberLevelEntity;
import com.guigu.gulimall.member.exception.PhoneExitException;
import com.guigu.gulimall.member.exception.UsernameExitException;
import com.guigu.gulimall.member.vo.MemberLoginVo;
import com.guigu.gulimall.member.vo.MemberRegistVo;
import com.guigu.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guigu.common.utils.PageUtils;
import com.guigu.common.utils.Query;

import com.guigu.gulimall.member.dao.MemberDao;
import com.guigu.gulimall.member.entity.MemberEntity;
import com.guigu.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public void regist(MemberRegistVo vo) {
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = new MemberEntity();
        //设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();   //获取默认的会员等级
        entity.setLevelId(levelEntity.getId());

        //用户名和手机号必须是唯一的，在保存数据之前来检查它的唯一性
        //检查用户名和手机号是否唯一。为了让controller能感知异常，使用异常机制,不同的错误抛不同的异常
        checkPhoneUnique(vo.getPhone());
        checkUsernameUnique(vo.getUserName());

        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());

        entity.setNickname(vo.getUserName());

        //密码要进行加密存储
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();//密码编码器
        String encode = passwordEncoder.encode(vo.getPassword());
        entity.setPassword(encode);

        //其他的默认信息

        //保存
        memberDao.insert(entity);
    }

    @Override
    public void checkUsernameUnique(String username) throws UsernameExitException {
        MemberDao memberDao = this.baseMapper;
        Integer count = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (count > 0) {
            throw new UsernameExitException();
        }
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExitException {
        MemberDao memberDao = this.baseMapper;
        Integer mobile = memberDao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (mobile > 0) {   //数据库有这条记录就抛异常
            throw new PhoneExitException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        //1.去数据库查询 select * from `ums_member` where username = ? or mobile = ?
        MemberDao memberDao = this.baseMapper;
        MemberEntity entity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct)
                .or().eq("mobile", loginacct));

        if(entity == null){
            //登录失败
            return null;
        } else {
            //1.获取到数据库的password
            String passwordDb = entity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //2.密码匹配
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if(matches){
                return entity;
            } else {
                return null;
            }
        }
    }

    //这个是使用社交账号来登录的
    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        //登录和注册合并逻辑
        String uid = socialUser.getUid();
        //1.判断当前社交用户是否已经登录过系统
        MemberDao memberDao = this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if(memberEntity != null) {
            //这个用户已经注册
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());

            memberDao.updateById(update);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;    //返回用户真正信息
        } else {
            //2.没有查到当前社交用户对应的记录，我们需要注册一个
            MemberEntity regist = new MemberEntity();
            try {    //防止网络不通的问题，远程查询社交信息不成功，也不影响它的社交登录，出现问题也没有关系
                //3.查询当前社交用户的社交账号信息(昵称、性别等)
                Map<String, String> query = new HashMap<>();
                query.put("access_token", socialUser.getAccess_token());
                query.put("uid", socialUser.getUid());
                HttpResponse response = HttpUtils.doGet("https://api.weibo.com", "/2/users/show.json", "get"
                        , new HashMap<String, String>(), query);
                if (response.getStatusLine().getStatusCode() == 200) {
                    //查询成功
                    String json = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = JSON.parseObject(json);
                    //当前社交账号所对应的昵称
                    String name = jsonObject.getString("name");
                    //性别
                    String gender = jsonObject.getString("gender");
                    //还有其他的信息...
                    regist.setNickname(name);
                    regist.setGender("m".equals(gender) ? 1 : 0);

                }
            } catch (Exception e) {}

            //关键是这几个
            regist.setSocialUid(socialUser.getUid());
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresIn(socialUser.getExpires_in());

            memberDao.insert(regist);

            return regist;
        }

    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

}