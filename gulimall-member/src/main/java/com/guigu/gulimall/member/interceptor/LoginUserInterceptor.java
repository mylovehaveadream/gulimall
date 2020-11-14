package com.guigu.gulimall.member.interceptor;

import com.guigu.common.constant.AuthServerConstant;
import com.guigu.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    //访问订单所有的请求都是登录后
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //   /member/memberreceiveaddress/info/{id}
        //  /order/order/status/{orderSn}   如果是请求路径是这样的，就放行
        String uri = request.getRequestURI();//就是请求后面的路径，上面的路径
        boolean match = new AntPathMatcher().match("/member/**", uri);//路径匹配器,这样的路径请求进行放行
        if(match){
            return true;    //直接放行
        }


        MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute != null) {
            loginUser.set(attribute);
            return true;
        } else {
            //没有登录就去登录
            request.getSession().setAttribute("msg","请先进行登录");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
}
