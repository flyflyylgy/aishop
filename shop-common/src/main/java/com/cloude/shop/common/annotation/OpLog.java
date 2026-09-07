package com.cloude.shop.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注需要审计的业务操作（登录/登出/注册/改密/下单/支付等）。
 * 与 {@link RequirePermission} 的区别：后者用于 RBAC 接口鉴权，
 * 本注解只负责审计留痕，两者可叠加使用。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {

    /** 操作类型编码，如 MEMBER_LOGIN / ORDER_CREATE */
    String value();
}
