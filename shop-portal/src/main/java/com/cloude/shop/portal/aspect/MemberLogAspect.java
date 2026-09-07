package com.cloude.shop.portal.aspect;

import com.cloude.shop.common.annotation.OpLog;
import com.cloude.shop.common.component.UserContext;
import com.cloude.shop.common.util.SensitiveLogUtil;
import com.cloude.shop.mapper.entity.UmsMemberLog;
import com.cloude.shop.mapper.mapper.UmsMemberLogMapper;
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
 * 会员业务操作审计切面：记录注册/登录/下单/支付/取消/收货/购物车等操作（成功/失败）。
 * 登录、注册、支付回调发生在登录态建立之前，UserContext 为空时 memberId 记 0，
 * 用户名从请求参数中提取，保证“谁在什么时间做了什么、是否成功、来源 IP”全程可追溯。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MemberLogAspect {

    private static final int PARAMS_MAX_LENGTH = 2000;
    private static final int ERROR_MAX_LENGTH = 500;

    private final UmsMemberLogMapper memberLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.cloude.shop.common.annotation.OpLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        OpLog opLog = method.getAnnotation(OpLog.class);
        String operation = opLog.value();
        try {
            Object result = joinPoint.proceed();
            saveLog(joinPoint, operation, true, null);
            return result;
        } catch (Throwable e) {
            saveLog(joinPoint, operation, false, e.getMessage());
            throw e;
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, String operation,
                         boolean success, String errorMsg) {
        try {
            UmsMemberLog logEntry = new UmsMemberLog();
            logEntry.setMemberId(UserContext.getUserId() != null ? UserContext.getUserId() : 0L);
            logEntry.setOperation(operation);

            String paramsJson = null;
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                logEntry.setMethod(request.getMethod());
                logEntry.setPath(request.getRequestURI());
                String xff = request.getHeader("X-Forwarded-For");
                logEntry.setIp(xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr());
            }
            try {
                paramsJson = SensitiveLogUtil.mask(objectMapper.writeValueAsString(joinPoint.getArgs()));
                logEntry.setParams(paramsJson.length() > PARAMS_MAX_LENGTH
                        ? paramsJson.substring(0, PARAMS_MAX_LENGTH) : paramsJson);
                if (logEntry.getUsername() == null) {
                    logEntry.setUsername(extractUsername(paramsJson));
                }
            } catch (Exception ignore) {
            }
            logEntry.setSuccess(success ? 1 : 0);
            if (errorMsg != null) {
                logEntry.setErrorMsg(errorMsg.length() > ERROR_MAX_LENGTH
                        ? errorMsg.substring(0, ERROR_MAX_LENGTH) : errorMsg);
            }
            memberLogMapper.insert(logEntry);
        } catch (Exception e) {
            // 审计失败不影响主流程
            log.error("会员操作日志写入失败", e);
        }
    }

    /** 从参数 JSON 中提取 username 字段（用于登录/注册等未建立登录态的场景） */
    private String extractUsername(String paramsJson) {
        try {
            var tree = objectMapper.readTree(paramsJson);
            if (tree.isArray()) {
                for (var node : tree) {
                    if (node != null && node.has("username") && !node.get("username").isNull()) {
                        return node.get("username").asText();
                    }
                }
            } else if (tree.has("username") && !tree.get("username").isNull()) {
                return tree.get("username").asText();
            }
        } catch (Exception ignore) {
        }
        return null;
    }
}
