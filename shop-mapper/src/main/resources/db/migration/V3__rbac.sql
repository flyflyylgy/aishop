-- ============================================================
-- V3 RBAC：角色 / 权限点 / 关联 / 管理操作审计日志
-- ============================================================

-- 角色表
CREATE TABLE `ums_role` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64) NOT NULL COMMENT '角色名称',
    `code`        VARCHAR(64) NOT NULL COMMENT '角色编码',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

-- 权限点表（接口级权限编码）
CREATE TABLE `ums_permission` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64) NOT NULL COMMENT '权限名称',
    `code`        VARCHAR(64) NOT NULL COMMENT '权限编码',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '权限点表';

-- 角色-权限关联
CREATE TABLE `ums_role_permission` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`       BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色-权限关联表';

-- 管理员-角色关联
CREATE TABLE `ums_admin_role` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `admin_id`   BIGINT NOT NULL COMMENT '管理员ID',
    `role_id`    BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_role` (`admin_id`, `role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员-角色关联表';

-- 管理操作审计日志
CREATE TABLE `ums_admin_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `admin_id`    BIGINT       NOT NULL COMMENT '管理员ID',
    `operation`   VARCHAR(64)  NOT NULL COMMENT '操作权限编码',
    `method`      VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP方法',
    `path`        VARCHAR(128) DEFAULT NULL COMMENT '请求路径',
    `params`      TEXT         COMMENT '请求参数(JSON,截断)',
    `ip`          VARCHAR(64)  DEFAULT NULL COMMENT '来源IP',
    `success`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功:0-失败 1-成功',
    `error_msg`   VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin` (`admin_id`),
    KEY `idx_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理操作审计日志表';

-- 种子：权限点
INSERT INTO `ums_permission` (`id`, `name`, `code`) VALUES
(1,  '商品查看',   'shop:product:list'),
(2,  '商品创建',   'shop:product:create'),
(3,  '商品更新',   'shop:product:update'),
(4,  '商品上下架', 'shop:product:status'),
(5,  '商品删除',   'shop:product:delete'),
(6,  '分类查看',   'shop:category:list'),
(7,  '分类创建',   'shop:category:create'),
(8,  '分类更新',   'shop:category:update'),
(9,  '分类删除',   'shop:category:delete'),
(10, '订单查看',   'shop:order:list'),
(11, '订单发货',   'shop:order:ship'),
(12, 'RBAC查看',   'shop:rbac:list');

-- 种子：角色
INSERT INTO `ums_role` (`id`, `name`, `code`, `description`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有全部权限(*)'),
(2, '运营人员',   'OPERATOR',    '商品/分类维护与订单查看');

-- 种子：角色-权限（SUPER_ADMIN 逻辑上拥有 *，物理绑定全部；OPERATOR 绑定部分）
INSERT INTO `ums_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `ums_permission`;

INSERT INTO `ums_role_permission` (`role_id`, `permission_id`) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 6), (2, 10);
