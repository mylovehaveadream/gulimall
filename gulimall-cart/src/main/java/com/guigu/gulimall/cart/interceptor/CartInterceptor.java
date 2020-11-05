package com.guigu.gulimall.cart.interceptor;

import com.guigu.common.constant.AuthServerConstant;
import com.guigu.common.constant.CartConstant;
import com.guigu.common.vo.MemberRespVo;
import com.guigu.gulimall.cart.vo.UserInfoTo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 在执行目标方法(controller的请求方法)之前，先来判断用户的登录状态，
 * 并封装传递给controller目标请求
 *
 * HandlerInterceptor:拦截器接口
 */
public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    //在目标方法执行之前拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserInfoTo userInfoTo = new UserInfoTo();   //传输对象,传给下一个用户用,页面不用的，用To

        HttpSession session = request.getSession(); //从redis中获取session
        MemberRespVo member = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(member != null) {
            //用户登录
            userInfoTo.setUserId(member.getId());
        }

        Cookie[] cookies = request.getCookies();
        if(cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                //user-key
                String name = cookie.getName();
                if(name.equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setTempUser(true);
                }
            }
        }

        //第一次，如果没有临时用户，帮忙创建一个临时用户,一定分配一个临时用户
        if(StringUtils.isEmpty(userInfoTo.getUserKey())){  //没有临时用户
            String uuid = UUID.randomUUID().toString(); //创建临时用户信息
            userInfoTo.setUserKey(uuid);
        }

        //目标方法执行之前,把信息放进threadLocal里面
        threadLocal.set(userInfoTo);

        return true;    //全部放行，来到目标方法都放行
    }


    //在业务执行之后，没有临时用户信息，就让浏览器保存一个cookie
    //分配临时用户，让浏览器保存
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserInfoTo userInfoTo = threadLocal.get();

        //如果没有临时用户，一定保存一个临时用户
        if(!userInfoTo.isTempUser()) {   //有临时用户信息，就没有必要放cookie了,反之；
            //持续的延长临时用户的过期时间
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("gulimall.com");   //cookie的作用域
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);   //cookie的过期时间

            response.addCookie(cookie);
        }
    }
}
