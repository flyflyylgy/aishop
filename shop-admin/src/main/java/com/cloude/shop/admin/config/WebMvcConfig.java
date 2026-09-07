package com.cloude.shop.admin.config;

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
 * 1. RateLimitInterceptor（最外层，限流）
 * 2. JwtInterceptor（登录态）
 * 3. AdminPermissionInterceptor（RBAC 权限）
 * + CORS 配置化（shop.cors.allowed-origins）
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final AdminPermissionInterceptor adminPermissionInterceptor;

    @Value("${shop.cors.allowed-origins:http://localhost:5173}")
    private String corsAllowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**");
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/admin/login", "/admin/captcha", "/doc.html", "/swagger-ui/**",
                        "/v3/api-docs/**", "/webjars/**");
        registry.addInterceptor(adminPermissionInterceptor).addPathPatterns("/**");
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
