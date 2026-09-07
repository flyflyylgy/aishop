package com.cloude.shop.admin.aspect;

import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.annotation.RequirePermission;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.common.util.SensitiveLogUtil;
import com.cloude.shop.mapper.entity.UmsAdminLog;
import com.cloude.shop.mapper.mapper.UmsAdminLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 管理操作审计切面：记录所有 @RequirePermission / @OpLog 接口的操作日志（成功/失败）。
 * 覆盖：商品/分类/订单/RBAC 等权限操作，以及管理员登录、登出、改密（含登录失败尝试）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminLogAspect {

    private static final int PARAMS_MAX_LENGTH = 2000;
    private static final int ERROR_MAX_LENGTH = 500;

    private final UmsAdminLogMapper adminLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.cloude.shop.common.annotation.RequirePermission) "
            + "|| @annotation(com.cloude.shop.common.annotation.OpLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        OpLog opLog = method.getAnnotation(OpLog.class);
        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
        String operation = opLog != null ? opLog.value() : requirePermission.value();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            saveLog(joinPoint, operation, true, null);
            log.info("管理操作完成: op={}, cost={}ms", operation, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            saveLog(joinPoint, operation, false, e.getMessage());
            throw e;
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, String operation,
                         boolean success, String errorMsg) {
        try {
            UmsAdminLog logEntry = new UmsAdminLog();
            logEntry.setAdminId(UserContext.getUserId() != null ? UserContext.getUserId() : 0L);
            logEntry.setOperation(operation);

            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                logEntry.setMethod(request.getMethod());
                logEntry.setPath(request.getRequestURI());
                String xff = request.getHeader("X-Forwarded-For");
                logEntry.setIp(xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr());
                try {
                    String params = SensitiveLogUtil.mask(objectMapper.writeValueAsString(joinPoint.getArgs()));
                    logEntry.setParams(params.length() > PARAMS_MAX_LENGTH
                            ? params.substring(0, PARAMS_MAX_LENGTH) : params);
                } catch (Exception ignore) {
                }
            }
            logEntry.setSuccess(success ? 1 : 0);
            if (errorMsg != null) {
                logEntry.setErrorMsg(errorMsg.length() > ERROR_MAX_LENGTH
                        ? errorMsg.substring(0, ERROR_MAX_LENGTH) : errorMsg);
            }
            adminLogMapper.insert(logEntry);
        } catch (Exception e) {
            // 审计失败不影响主流程
            log.error("审计日志写入失败", e);
        }
    }
}
