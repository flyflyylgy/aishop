package com.cloude.shop.common.constant;

/**
 * Redis Key 常量
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {
    }

    /** 会员登录 token（可吊销登录态） */
    public static final String MEMBER_TOKEN = "shop:token:member:";
    /** 管理员登录 token */
    public static final String ADMIN_TOKEN = "shop:token:admin:";
    /** 商品详情缓存 */
    public static final String PRODUCT_CACHE = "shop:product:";
    /** 商品分类树缓存 */
    public static final String CATEGORY_TREE_CACHE = "shop:category:tree";
    /** 首页推荐商品缓存 */
    public static final String HOT_PRODUCT_CACHE = "shop:product:hot";
    /** 下单幂等键 */
    public static final String ORDER_IDEM_PREFIX = "shop:idem:order:";
    /** 管理员权限集缓存 */
    public static final String ADMIN_PERM_PREFIX = "shop:perm:admin:";
    /** 商品详情空值占位（防穿透） */
    public static final String NULL_PLACEHOLDER = "null";
    /** 限流 key 前缀 */
    public static final String RATE_LIMIT_PREFIX = "shop:rate:";
    /** 图形验证码 key 前缀 */
    public static final String CAPTCHA_PREFIX = "shop:captcha:";
    /** 登录失败计数 key 前缀 */
    public static final String LOGIN_FAIL_PREFIX = "shop:loginfail:";

    /** token 有效期（秒） */
    public static final long TOKEN_EXPIRE_SECONDS = 24 * 60 * 60;
    /** 商品详情缓存 1 小时 */
    public static final long PRODUCT_EXPIRE_SECONDS = 60 * 60;
}
