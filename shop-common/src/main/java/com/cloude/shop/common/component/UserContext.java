package com.cloude.shop.common.component;

/**
 * 当前登录用户上下文（由 JWT 拦截器填充）
 */
public final class UserContext {

    private UserContext() {
    }

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void set(Long userId, String role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    /** 当前用户 ID（拦截器保证已登录时非空） */
    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
