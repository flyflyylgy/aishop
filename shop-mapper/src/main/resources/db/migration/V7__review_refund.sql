-- ==================== 商品评价 ====================
CREATE TABLE pms_review (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id    BIGINT       NOT NULL COMMENT '商品ID',
    member_id     BIGINT       NOT NULL COMMENT '会员ID',
    member_name   VARCHAR(64)  NOT NULL COMMENT '会员昵称（快照）',
    order_id      BIGINT       NOT NULL COMMENT '订单ID',
    order_no      VARCHAR(64)  NOT NULL COMMENT '订单号',
    rating        TINYINT      NOT NULL COMMENT '评分 1-5',
    content       VARCHAR(500) NOT NULL COMMENT '评价内容',
    pics          VARCHAR(1000) DEFAULT NULL COMMENT '晒图URL，逗号分隔',
    delete_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_product (product_id),
    KEY idx_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价';

-- ==================== 售后退款单 ====================
CREATE TABLE oms_refund (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id        BIGINT       NOT NULL COMMENT '订单ID',
    order_no        VARCHAR(64)  NOT NULL COMMENT '订单号',
    member_id       BIGINT       NOT NULL COMMENT '会员ID',
    reason          VARCHAR(200) NOT NULL COMMENT '退款原因',
    amount          DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0-待审核 1-已同意 2-已拒绝',
    admin_remark    VARCHAR(200) DEFAULT NULL COMMENT '管理员备注',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    handle_time     DATETIME     DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后退款单';

-- 新增权限点：评价管理、退款审批
INSERT INTO ums_permission (id, name, code) VALUES
(22, '评价管理', 'shop:review:list'),
(23, '退款审批', 'shop:refund:handle');

INSERT INTO ums_role_permission (role_id, permission_id) VALUES (1, 22), (1, 23);
