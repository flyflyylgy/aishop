package com.cloude.shop.portal.config;

import com.cloude.shop.common.component.JwtInterceptor;
import com.cloude.shop.common.component.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：
 * 1. RateLimitInterceptor（限流：敏感接口 60s/10 次，其余 10s/100 次）
 * 2. JwtInterceptor（会员相关接口需要登录，商品/分类浏览开放）
 * + CORS 配置化（shop.cors.allowed-origins）
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Value("${shop.cors.allowed-origins:http://localhost:5173}")
    private String corsAllowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/member/**", "/cart/**", "/order/**", "/pay/**", "/address/**")
                .excludePathPatterns("/member/login", "/member/register", "/member/captcha",
                        "/member/forgot/**", "/pay/notify", "/doc.html",
                        "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(corsAllowedOrigins.split(","))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
