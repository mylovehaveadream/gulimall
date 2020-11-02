package com.guigu.gulimall.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//定制一些springMVC的功能
@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {

    //视图映射
    //直接告诉他哪个请求映射哪个页面，无需写controller了
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        //那些页面跳到哪，在这里面进行注册就行了
        /**
         *  @GetMapping("/login.html")
         *  public String loginPage(){
         *       return "login";
         *  }
         */
//        registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("/reg.html").setViewName("reg");
    }
}
