package com.guigu.gulimall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GulimallCorsConfiguration {
    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        //1.配置跨越
        corsConfiguration.addAllowedHeader("*");    //允许那些头进行跨域
        corsConfiguration.addAllowedMethod("*");    //允许那些请求方式进行跨域
        corsConfiguration.addAllowedOrigin("*");    //允许那些来请求源进行跨域
        corsConfiguration.setAllowCredentials(true);  //是否允许携带cookie进行跨域

        // /**:任意路径进行跨域的配置
        source.registerCorsConfiguration("/**",corsConfiguration);

        return new CorsWebFilter(source);
    }
}
