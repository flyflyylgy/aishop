package com.cloude.shop.common.util;

import java.util.regex.Pattern;

/**
 * 审计日志敏感信息脱敏：JSON 参数中的密码/令牌字段一律掩码，禁止明文落库。
 */
public final class SensitiveLogUtil {

    private SensitiveLogUtil() {
    }

    /** 匹配 "password"/"oldPassword"/"newPassword"/"token" 等字段的字符串值 */
    private static final Pattern SENSITIVE_FIELD = Pattern.compile(
            "(\"(?:[a-zA-Z]*[pP]assword|token|secret|idempotency-?key)\"\\s*:\\s*\")(?:[^\"\\\\]|\\\\.)*(\")");

    private static final String MASK = "******";

    /**
     * 对 JSON 字符串中的敏感字段脱敏；非 JSON 或为空时原样返回。
     */
    public static String mask(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        return SENSITIVE_FIELD.matcher(json).replaceAll("$1" + MASK + "$2");
    }
}
