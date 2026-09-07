-- ============================================================
-- V10 站内信：会员消息通知（订单/支付/物流/售后/系统）
-- 由关键业务节点写入（支付成功、发货、关单、退款审批等），不依赖外部短信/邮件
-- ============================================================

CREATE TABLE ums_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    member_id   BIGINT       NOT NULL COMMENT '会员ID',
    type        TINYINT      NOT NULL DEFAULT 5 COMMENT '类型:1-订单 2-支付 3-物流 4-售后 5-系统',
    title       VARCHAR(128) NOT NULL COMMENT '标题',
    content     VARCHAR(500) DEFAULT NULL COMMENT '内容',
    biz_type    VARCHAR(32)  DEFAULT NULL COMMENT '业务类型:order/refund',
    biz_id      VARCHAR(64)  DEFAULT NULL COMMENT '业务ID(订单号等)',
    is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读:0-未读 1-已读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_member_read (member_id, is_read),
    KEY idx_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员站内信';
