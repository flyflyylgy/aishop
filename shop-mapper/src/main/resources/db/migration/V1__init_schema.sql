-- ============================================================
-- V1 初始建表：Cloude Shop 企业级商城
-- ============================================================

-- 后台管理员
CREATE TABLE `ums_admin` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    `nick_name`   VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态:0-禁用 1-启用',
    `delete_flag` TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0-未删 1-已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '后台管理员表';

-- 前台会员
CREATE TABLE `ums_member` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)   NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100)  NOT NULL COMMENT '密码(BCrypt)',
    `nickname`    VARCHAR(64)   DEFAULT NULL COMMENT '昵称',
    `phone`       VARCHAR(20)   DEFAULT NULL COMMENT '手机号',
    `icon`        VARCHAR(255)  DEFAULT NULL COMMENT '头像',
    `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '账号状态:0-禁用 1-启用',
    `delete_flag` TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '会员表';

-- 商品分类
CREATE TABLE `pms_category` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类ID,0为一级分类',
    `name`        VARCHAR(64)  NOT NULL COMMENT '分类名称',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `show_flag`   TINYINT      NOT NULL DEFAULT 1 COMMENT '是否显示:0-隐藏 1-显示',
    `delete_flag` TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品分类表';

-- 品牌
CREATE TABLE `pms_brand` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64)  NOT NULL COMMENT '品牌名称',
    `logo`        VARCHAR(255) DEFAULT NULL COMMENT '品牌Logo',
    `story`       TEXT         COMMENT '品牌故事',
    `delete_flag` TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '品牌表';

-- 商品（库存三态分离：available / locked / sold，乐观扣减防超卖）
CREATE TABLE `pms_product` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_id`     BIGINT         NOT NULL COMMENT '分类ID',
    `brand_id`        BIGINT         DEFAULT NULL COMMENT '品牌ID',
    `name`            VARCHAR(200)   NOT NULL COMMENT '商品名称',
    `sub_title`       VARCHAR(200)   DEFAULT NULL COMMENT '副标题',
    `main_image`      VARCHAR(255)   DEFAULT NULL COMMENT '主图URL',
    `sub_images`      TEXT           COMMENT '副图URL,逗号分隔',
    `price`           DECIMAL(10, 2) NOT NULL COMMENT '现价',
    `original_price`  DECIMAL(10, 2) DEFAULT NULL COMMENT '原价',
    `detail_html`     TEXT           COMMENT '商品详情HTML',
    -- 库存三态分离
    `available_stock` INT            NOT NULL DEFAULT 0 COMMENT '可售库存',
    `locked_stock`    INT            NOT NULL DEFAULT 0 COMMENT '锁定库存(已下单未支付)',
    `sold_stock`      INT            NOT NULL DEFAULT 0 COMMENT '已售库存',
    `sale`            INT            NOT NULL DEFAULT 0 COMMENT '销量(冗余展示)',
    `status`          TINYINT        NOT NULL DEFAULT 1 COMMENT '上架状态:0-下架 1-上架',
    `delete_flag`     TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`),
    KEY `idx_brand` (`brand_id`),
    KEY `idx_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品表';

-- 购物车
CREATE TABLE `oms_cart_item` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `member_id`   BIGINT         NOT NULL COMMENT '会员ID',
    `product_id`  BIGINT         NOT NULL COMMENT '商品ID',
    `quantity`    INT            NOT NULL DEFAULT 1 COMMENT '数量',
    `price`       DECIMAL(10, 2) NOT NULL COMMENT '加入时价格',
    `selected`    TINYINT        NOT NULL DEFAULT 1 COMMENT '是否勾选:0-否 1-是',
    `delete_flag` TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_member_product` (`member_id`, `product_id`),
    KEY `idx_member` (`member_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '购物车表';

-- 订单（状态机：0待付款 1已付款 2已发货 3已完成 4已关闭）
CREATE TABLE `oms_order` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`       VARCHAR(64)    NOT NULL COMMENT '订单号',
    `member_id`      BIGINT         NOT NULL COMMENT '会员ID',
    `status`         TINYINT        NOT NULL DEFAULT 0 COMMENT '状态:0-待付款 1-已付款 2-已发货 3-已完成 4-已关闭',
    `total_amount`   DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    `pay_amount`     DECIMAL(10, 2) NOT NULL COMMENT '实付金额',
    `receiver_name`  VARCHAR(64)    NOT NULL COMMENT '收货人',
    `receiver_phone` VARCHAR(20)    NOT NULL COMMENT '联系电话',
    `receiver_addr`  VARCHAR(255)   NOT NULL COMMENT '收货地址',
    `note`           VARCHAR(255)   DEFAULT NULL COMMENT '订单备注',
    `pay_type`       TINYINT        DEFAULT NULL COMMENT '支付方式:0-模拟支付',
    `pay_time`       DATETIME       DEFAULT NULL COMMENT '支付时间',
    `ship_time`      DATETIME       DEFAULT NULL COMMENT '发货时间',
    `finish_time`    DATETIME       DEFAULT NULL COMMENT '完成时间',
    `close_time`     DATETIME       DEFAULT NULL COMMENT '关闭时间',
    `close_reason`   VARCHAR(255)   DEFAULT NULL COMMENT '关闭原因',
    `create_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_member` (`member_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单表';

-- 订单明细
CREATE TABLE `oms_order_item` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`    BIGINT         NOT NULL COMMENT '订单ID',
    `order_no`    VARCHAR(64)    NOT NULL COMMENT '订单号',
    `product_id`  BIGINT         NOT NULL COMMENT '商品ID',
    `product_pic` VARCHAR(255)   DEFAULT NULL COMMENT '商品图片',
    `product_name` VARCHAR(200)  NOT NULL COMMENT '商品名称(下单快照)',
    `price`       DECIMAL(10, 2) NOT NULL COMMENT '成交单价',
    `quantity`    INT            NOT NULL COMMENT '购买数量',
    `total_price` DECIMAL(10, 2) NOT NULL COMMENT '小计金额',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细表';

-- 支付回调日志（幂等核对 + 审计）
CREATE TABLE `oms_pay_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `pay_no`       VARCHAR(64)  NOT NULL COMMENT '第三方支付流水号',
    `order_no`     VARCHAR(64)  NOT NULL COMMENT '订单号',
    `amount`       DECIMAL(10, 2) NOT NULL COMMENT '支付金额',
    `success`      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功:0-失败 1-成功',
    `callback_body` TEXT        COMMENT '回调报文',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回调时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_no` (`pay_no`),
    KEY `idx_order_no` (`order_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '支付回调日志表';
