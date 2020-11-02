package com.guigu.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.guigu.common.utils.HttpUtils;
import com.guigu.common.utils.R;
import com.guigu.gulimall.auth.feign.MemberFeignService;
import com.guigu.common.vo.MemberRespVo;
import com.guigu.gulimall.auth.vo.SocialUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;

/**
 * 处理社交登录请求
 */
@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    //社交登录成功回调
    @GetMapping("/oauth2.0/weibo/sucess")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {
        HashMap<String, String> map = new HashMap<>();
        map.put("client_id","2636917288");
        map.put("client_secret","6a26XXXXXXXXXXX");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http://auth.gulimall.com/oauth2.0/weibo/sucess");
        map.put("code",code);

        //1.根据code换取Access Token
        HttpResponse response =
                HttpUtils.doPost("api.weibo.com", "/oauth/access_token", "post", null, null, map);

        //2.处理
        if(response.getStatusLine().getStatusCode() == 200) {
            //换取到了Access Token
            String json = EntityUtils.toString(response.getEntity());
            //将JSON转成对应的对象
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

            //知道当前是哪个社交用户
            //1.当前用户如果是第一次进网站，自动注册进来(为当前社交用户生成一个会员信息账号，以后这个社交账号就对应指定的会员)
            //登录或者注册这个社交用户
            R oauthlogin = memberFeignService.oauthlogin(socialUser);
            if(oauthlogin.getCode() == 0){
                MemberRespVo data = oauthlogin.getData("data", new TypeReference<MemberRespVo>() {});
                log.info("登录成功，用户信息是：" , data.toString());
                //1.第一次使用session，命令浏览器保存卡号，就是JSESSIONID这个cookie
                //以后浏览器访问哪个网站就会带上这个网站的cookie
                //子域之间；gulimall.com  auth.gulimall.com  order.gulimall.com
                //发卡的时候（指定域名为父域名），即使是子域系统发的卡，也能让父域直接使用
                //TODO 1、默认发的令牌，session=xxxx。作用域：当前域（解决子域session共享问题）
                //TODO 2、使用JSON的序列化方式来序列化对象数据放到redis中
                session.setAttribute("loginUser",data);
                //2.登录成功就跳回首页
                return "redirect:http://gulimall.com";
            } else {
                return "redirect:http://auth.gulimall.com/login.html";
            }

        } else {
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
