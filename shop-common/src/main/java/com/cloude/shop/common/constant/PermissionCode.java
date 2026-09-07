package com.cloude.shop.common.constant;

/**
 * 权限编码常量（与 ums_permission 种子数据一致）
 */
public final class PermissionCode {

    private PermissionCode() {
    }

    /** 超级管理员通配权限 */
    public static final String ALL = "*";

    public static final String PRODUCT_LIST = "shop:product:list";
    public static final String PRODUCT_CREATE = "shop:product:create";
    public static final String PRODUCT_UPDATE = "shop:product:update";
    public static final String PRODUCT_STATUS = "shop:product:status";
    public static final String PRODUCT_DELETE = "shop:product:delete";
    public static final String CATEGORY_LIST = "shop:category:list";
    public static final String CATEGORY_CREATE = "shop:category:create";
    public static final String CATEGORY_UPDATE = "shop:category:update";
    public static final String CATEGORY_DELETE = "shop:category:delete";
    public static final String ORDER_LIST = "shop:order:list";
    public static final String ORDER_SHIP = "shop:order:ship";
    public static final String RBAC_LIST = "shop:rbac:list";
    public static final String MEMBER_LOG = "shop:log:member";
    public static final String ADMIN_LIST = "shop:admin:list";
    public static final String ADMIN_CREATE = "shop:admin:create";
    public static final String ADMIN_UPDATE = "shop:admin:update";
    public static final String ADMIN_ASSIGN = "shop:admin:assign";
    public static final String ROLE_ASSIGN = "shop:role:assign";
    public static final String MEMBER_LIST = "shop:member:list";
    public static final String MEMBER_STATUS = "shop:member:status";
    public static final String ORDER_REMARK = "shop:order:remark";
    public static final String REVIEW_LIST = "shop:review:list";
    public static final String REFUND_HANDLE = "shop:refund:handle";
    public static final String COUPON_LIST = "shop:coupon:list";
    public static final String COUPON_CREATE = "shop:coupon:create";
    public static final String COUPON_UPDATE = "shop:coupon:update";
    public static final String COUPON_STATUS = "shop:coupon:status";
}
