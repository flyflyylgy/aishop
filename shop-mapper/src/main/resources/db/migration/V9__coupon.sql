-- ============================================================
-- V9 营销体系：优惠券（满减/折扣/现金）+ 会员领券记录 + 订单优惠抵扣
-- 设计参考 mall-sms：券模板(sms_coupon) + 领券流水(sms_coupon_history)
-- 订单下单时校验门槛并核销，关单(超时/取消)时回退为未使用
-- ============================================================

-- 优惠券模板 / 活动
CREATE TABLE sms_coupon (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name           VARCHAR(64)   NOT NULL COMMENT '券名称',
    type           TINYINT       NOT NULL COMMENT '类型:1-满减券 2-折扣券 3-现金券(无门槛)',
    face_value     DECIMAL(10,2) DEFAULT NULL COMMENT '面额(满减/现金券抵扣金额)',
    discount       INT           DEFAULT NULL COMMENT '折扣(折扣券:如85表示8.5折,取值1-99)',
    min_point      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '使用门槛金额(0=无门槛)',
    total_count    INT           NOT NULL DEFAULT 0 COMMENT '发行总量',
    received_count INT           NOT NULL DEFAULT 0 COMMENT '已领取数量',
    used_count     INT           NOT NULL DEFAULT 0 COMMENT '已使用数量',
    per_limit      INT           NOT NULL DEFAULT 1 COMMENT '每人限领张数',
    start_time     DATETIME      NOT NULL COMMENT '领取开始时间',
    end_time       DATETIME      NOT NULL COMMENT '领取结束时间',
    valid_days     INT           NOT NULL DEFAULT 30 COMMENT '领取后有效天数',
    status         TINYINT       NOT NULL DEFAULT 1 COMMENT '0-下架 1-上架',
    delete_flag    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status_end (status, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券';

-- 会员领券记录
CREATE TABLE sms_coupon_history (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    coupon_id    BIGINT       NOT NULL COMMENT '优惠券ID',
    member_id    BIGINT       NOT NULL COMMENT '会员ID',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0-未使用 1-已使用 2-已过期',
    order_no     VARCHAR(32)  DEFAULT NULL COMMENT '使用订单号',
    receive_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    use_time     DATETIME     DEFAULT NULL COMMENT '使用时间',
    expire_time  DATETIME     NOT NULL COMMENT '过期时间',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_member_status (member_id, status),
    KEY idx_coupon (coupon_id),
    KEY idx_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员优惠券';

-- 订单增加优惠抵扣字段
ALTER TABLE oms_order ADD COLUMN coupon_id     BIGINT        DEFAULT NULL COMMENT '优惠券ID' AFTER pay_amount;
ALTER TABLE oms_order ADD COLUMN coupon_name   VARCHAR(64)   DEFAULT NULL COMMENT '优惠券名称快照' AFTER coupon_id;
ALTER TABLE oms_order ADD COLUMN coupon_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠抵扣金额' AFTER coupon_name;

-- 新增权限点：优惠券管理
INSERT INTO ums_permission (id, name, code) VALUES
(24, '优惠券查看',   'shop:coupon:list'),
(25, '优惠券创建',   'shop:coupon:create'),
(26, '优惠券更新',   'shop:coupon:update'),
(27, '优惠券上下架', 'shop:coupon:status');

INSERT INTO ums_role_permission (role_id, permission_id) VALUES (1, 24), (1, 25), (1, 26), (1, 27);
