-- ============================================================
-- V5 后台管理闭环：订单物流/备注字段 + 管理员/会员/角色授权权限点
-- ============================================================

-- 订单物流与后台备注
ALTER TABLE `oms_order`
    ADD COLUMN `express_company` VARCHAR(64)  DEFAULT NULL COMMENT '物流公司' AFTER `ship_time`,
    ADD COLUMN `express_no`      VARCHAR(64)  DEFAULT NULL COMMENT '物流单号' AFTER `express_company`,
    ADD COLUMN `admin_remark`    VARCHAR(255) DEFAULT NULL COMMENT '后台备注' AFTER `close_reason`;

-- 新增权限点
INSERT INTO `ums_permission` (`id`, `name`, `code`) VALUES
(14, '管理员列表',   'shop:admin:list'),
(15, '新增管理员',   'shop:admin:create'),
(16, '编辑管理员',   'shop:admin:update'),
(17, '分配角色',     'shop:admin:assign'),
(18, '角色权限配置', 'shop:role:assign'),
(19, '会员列表',     'shop:member:list'),
(20, '会员封禁解封', 'shop:member:status'),
(21, '订单备注',     'shop:order:remark');

-- 物理绑定给超级管理员角色（代码层 SUPER_ADMIN 拥有 * 通配，此处便于查询展示）
INSERT INTO `ums_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `ums_permission` WHERE id BETWEEN 14 AND 21;
