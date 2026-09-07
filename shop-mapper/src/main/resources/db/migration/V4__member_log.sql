-- ============================================================
-- V4 会员业务操作日志 + 会员日志查看权限
-- ============================================================

-- 会员业务操作日志（注册/登录/下单/支付/取消/收货/购物车等）
CREATE TABLE `ums_member_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '会员ID（登录/注册时为0，取用户名）',
    `username`    VARCHAR(64)  DEFAULT NULL COMMENT '会员账号（冗余，便于登录失败也可追溯）',
    `operation`   VARCHAR(64)  NOT NULL COMMENT '操作类型，如 MEMBER_LOGIN/ORDER_CREATE',
    `method`      VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP方法',
    `path`        VARCHAR(128) DEFAULT NULL COMMENT '请求路径',
    `params`      TEXT         COMMENT '请求参数(JSON,敏感字段脱敏,截断)',
    `ip`          VARCHAR(64)  DEFAULT NULL COMMENT '来源IP',
    `success`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功:0-失败 1-成功',
    `error_msg`   VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_member` (`member_id`),
    KEY `idx_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员业务操作日志表';

-- 新增权限点：会员日志查看（仅超级管理员；OPERATOR 不授予）
INSERT INTO `ums_permission` (`id`, `name`, `code`) VALUES
(13, '会员日志查看', 'shop:log:member');

-- 物理绑定给超级管理员角色（代码层 SUPER_ADMIN 本身拥有 * 通配，此处便于查询展示）
INSERT INTO `ums_role_permission` (`role_id`, `permission_id`) VALUES (1, 13);
