package com.cloude.shop.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 后台接口权限声明（AdminPermissionInterceptor 校验，AdminLogAspect 审计）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 权限编码，如 shop:product:create */
    String value();
}
