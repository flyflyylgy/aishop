-- 会员收货地址簿
CREATE TABLE ums_member_address (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    member_id     BIGINT       NOT NULL COMMENT '会员ID',
    receiver_name VARCHAR(64)  NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    receiver_addr VARCHAR(255) NOT NULL COMMENT '收货地址',
    is_default    TINYINT      NOT NULL DEFAULT 0 COMMENT '0-普通 1-默认地址',
    delete_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员收货地址';
