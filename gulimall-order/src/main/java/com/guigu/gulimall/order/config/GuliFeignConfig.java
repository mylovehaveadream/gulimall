package com.guigu.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class GuliFeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() { //给容器中放一个拦截器，他们就会从容器中自动的获取
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                //1.RequestContextHolder拿到刚进来的这个请求
                //上下文环境的保持器，底层也还是使用ThreadLocal
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();//可以拿到当前的所有请求属性
                if(attributes != null) {
                    HttpServletRequest request = attributes.getRequest();   //获取当前的请求对象，老请求
                    if (request != null) {
                        //同步请求头数据，cookie
                        String cookie = request.getHeader("Cookie");
                        //给新请求同步了老请求的cookie
                        requestTemplate.header("Cookie", cookie);
                    }
                }

                System.out.println("feign远程之前先进行RequestInterceptor.apply");
            }
        };
    }
}
