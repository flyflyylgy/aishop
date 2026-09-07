-- ============================================================
-- V8 商品 SKU 多规格支持
-- 向后兼容：has_sku=0 的商品保持原单规格行为；has_sku=1 库存/价格下沉到 SKU
-- ============================================================

-- pms_product 增加多规格标志和维度名
ALTER TABLE pms_product ADD COLUMN has_sku TINYINT NOT NULL DEFAULT 0 COMMENT '是否多规格:0-否 1-是' AFTER sale;
ALTER TABLE pms_product ADD COLUMN spec_names VARCHAR(500) DEFAULT NULL COMMENT '规格维度名(逗号分隔:颜色,内存)' AFTER has_sku;

-- SKU 表
CREATE TABLE pms_sku (
  id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  product_id      BIGINT        NOT NULL COMMENT '商品ID',
  sku_code        VARCHAR(64)   DEFAULT NULL COMMENT '商家SKU编码',
  spec_values     VARCHAR(500)  NOT NULL COMMENT '规格值JSON:{"颜色":"红","内存":"8G"}',
  price           DECIMAL(10,2) NOT NULL COMMENT 'SKU价格',
  available_stock INT           NOT NULL DEFAULT 0 COMMENT '可售库存',
  locked_stock    INT           NOT NULL DEFAULT 0 COMMENT '锁定库存(已下单未支付)',
  sold_stock      INT           NOT NULL DEFAULT 0 COMMENT '已售',
  status          TINYINT       NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
  delete_flag     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU';

-- 购物车增加 nullable sku_id
ALTER TABLE oms_cart_item ADD COLUMN sku_id BIGINT DEFAULT NULL COMMENT 'SKU ID' AFTER product_id;

-- 订单明细增加 sku_id 和规格快照
ALTER TABLE oms_order_item ADD COLUMN sku_id BIGINT DEFAULT NULL COMMENT 'SKU ID' AFTER product_id;
ALTER TABLE oms_order_item ADD COLUMN spec_values VARCHAR(500) DEFAULT NULL COMMENT '规格快照JSON' AFTER sku_id;
