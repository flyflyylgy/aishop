package com.cloude.shop.admin.config;

import com.cloude.shop.common.annotation.RequirePermission;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.service.service.RbacService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * RBAC 权限拦截器：校验 @RequirePermission 声明的权限编码
 * 依赖 JwtInterceptor 先行填充 UserContext
 */
@Component
@RequiredArgsConstructor
public class AdminPermissionInterceptor implements HandlerInterceptor {

    private final RbacService rbacService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            return true;
        }
        Long adminId = UserContext.getUserId();
        if (adminId == null) {
            return reject(response, 401, "未登录");
        }
        Set<String> perms = rbacService.loadPermissions(adminId);
        if (perms.contains(com.cloude.shop.common.constant.PermissionCode.ALL)
                || perms.contains(annotation.value())) {
            return true;
        }
        return reject(response, 403, "没有操作权限: " + annotation.value());
    }

    private boolean reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream().write(new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsBytes(com.cloude.shop.common.api.CommonResult.forbidden(message)));
        response.getOutputStream().flush();
        return false;
    }
}
