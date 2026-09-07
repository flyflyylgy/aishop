# 数据库设计文档

> 库 `cloude_shop`，MySQL 8.0，InnoDB 引擎，utf8mb4 字符集。由 Flyway 自动迁移（V1~V5）。

## 表关系总览

```
ums_admin ──┐              ┌── ums_role
            │              │        │
ums_admin_role ───────────┘        │
                                   │
                          ums_role_permission ── ums_permission
                                   
ums_member ──── oms_cart_item ──── pms_product ──── pms_category
     │                                  │
     │                                  └── pms_brand
     │
     └── oms_order ──── oms_order_item
              │
              └── oms_pay_log

ums_admin_log（独立审计）
ums_member_log（独立审计）
```

## 表结构明细

### ums_admin（后台管理员）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(64) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(100) | NOT NULL | 密码（BCrypt 哈希） |
| nick_name | VARCHAR(64) | | 昵称 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 0-禁用 1-启用 |
| delete_flag | TINYINT | NOT NULL, DEFAULT 0 | 逻辑删除 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW | 创建时间 |
| update_time | DATETIME | NOT NULL, ON UPDATE NOW | 更新时间 |

### ums_member（会员）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(64) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(100) | NOT NULL | 密码（BCrypt 哈希） |
| nickname | VARCHAR(64) | | 昵称 |
| phone | VARCHAR(20) | | 手机号 |
| icon | VARCHAR(255) | | 头像 |
| status | TINYINT | NOT NULL, DEFAULT 1 | 0-封禁 1-正常 |
| delete_flag | TINYINT | NOT NULL, DEFAULT 0 | 逻辑删除 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW | 创建时间 |
| update_time | DATETIME | NOT NULL, ON UPDATE NOW | 更新时间 |

### ums_role（角色）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(64) | NOT NULL | 角色名称 |
| code | VARCHAR(64) | NOT NULL, UNIQUE | 角色编码（SUPER_ADMIN/OPERATOR） |
| description | VARCHAR(200) | | 描述 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW | 创建时间 |

### ums_permission（权限点）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(64) | NOT NULL | 权限名称 |
| code | VARCHAR(64) | NOT NULL, UNIQUE | 权限编码（如 shop:product:list） |
| create_time | DATETIME | NOT NULL, DEFAULT NOW | 创建时间 |

**权限点清单（21 项）**：

| ID | code | name |
|---|---|---|
| 1 | shop:product:list | 商品查看 |
| 2 | shop:product:create | 商品创建 |
| 3 | shop:product:update | 商品更新 |
| 4 | shop:product:status | 商品上下架 |
| 5 | shop:product:delete | 商品删除 |
| 6 | shop:category:list | 分类查看 |
| 7 | shop:category:create | 分类创建 |
| 8 | shop:category:update | 分类更新 |
| 9 | shop:category:delete | 分类删除 |
| 10 | shop:order:list | 订单查看 |
| 11 | shop:order:ship | 订单发货 |
| 12 | shop:rbac:list | RBAC 查看 |
| 13 | shop:log:member | 会员日志查看 |
| 14 | shop:admin:list | 管理员列表 |
| 15 | shop:admin:create | 新增管理员 |
| 16 | shop:admin:update | 编辑管理员 |
| 17 | shop:admin:assign | 分配角色 |
| 18 | shop:role:assign | 角色权限配置 |
| 19 | shop:member:list | 会员列表 |
| 20 | shop:member:status | 会员封禁解封 |
| 21 | shop:order:remark | 订单备注 |

### ums_role_permission（角色-权限关联）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| role_id | BIGINT | NOT NULL, UNIQUE(role_id,permission_id) | 角色ID |
| permission_id | BIGINT | NOT NULL | 权限ID |

### ums_admin_role（管理员-角色关联）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| admin_id | BIGINT | NOT NULL, UNIQUE(admin_id,role_id) | 管理员ID |
| role_id | BIGINT | NOT NULL | 角色ID |

### pms_category（商品分类）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| parent_id | BIGINT | NOT NULL, DEFAULT 0, INDEX | 父分类ID（0=一级） |
| name | VARCHAR(64) | NOT NULL | 分类名称 |
| sort | INT | NOT NULL, DEFAULT 0 | 排序 |
| show_flag | TINYINT | NOT NULL, DEFAULT 1 | 0-隐藏 1-显示 |
| delete_flag | TINYINT | NOT NULL, DEFAULT 0 | 逻辑删除 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW | 创建时间 |

### pms_brand（品牌）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(64) | NOT NULL | 品牌名称 |
| logo | VARCHAR(255) | | Logo URL |
| story | TEXT | | 品牌故事 |
| delete_flag | TINYINT | NOT NULL, DEFAULT 0 | 逻辑删除 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW | 创建时间 |

### pms_product（商品）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| category_id | BIGINT | NOT NULL, INDEX | 分类ID |
| brand_id | BIGINT | INDEX | 品牌ID |
| name | VARCHAR(200) | NOT NULL, INDEX | 商品名称 |
| sub_title | VARCHAR(200) | | 副标题 |
| main_image | VARCHAR(255) | | 主图 URL |
| sub_images | TEXT | | 副图 URL（逗号分隔） |
| price | DECIMAL(10,2) | NOT NULL | 现价 |
| original_price | DECIMAL(10,2) | | 原价 |
| detail_html | TEXT | | 详情 HTML |
| available_stock | INT | NOT NULL, DEFAULT 0 | 可售库存 |
| locked_stock | INT | NOT NULL, DEFAULT 0 | 锁定库存（已下单未支付） |
| sold_stock | INT | NOT NULL, DEFAULT 0 | 已售库存 |
| sale | INT | NOT NULL, DEFAULT 0 | 销量（冗余展示） |
| status | TINYINT | NOT NULL, DEFAULT 1 | 0-下架 1-上架 |
| delete_flag | TINYINT | NOT NULL, DEFAULT 0 | 逻辑删除 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW | 创建时间 |
| update_time | DATETIME | NOT NULL, ON UPDATE NOW | 更新时间 |

> **库存三态分离**：下单 `available→locked`，支付 `locked→sold`，关单 `locked→available`，CAS 条件更新防超卖。

### oms_cart_item（购物车）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| member_id | BIGINT | NOT NULL, INDEX | 会员ID |
| product_id | BIGINT | NOT NULL | 商品ID |
| quantity | INT | NOT NULL, DEFAULT 1 | 数量 |
| price | DECIMAL(10,2) | NOT NULL | 加入时价格 |
| selected | TINYINT | NOT NULL, DEFAULT 1 | 0-未勾选 1-已勾选 |
| delete_flag | TINYINT | NOT NULL, DEFAULT 0 | 逻辑删除 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW | 创建时间 |
| update_time | DATETIME | NOT NULL, ON UPDATE NOW | 更新时间 |

> UNIQUE(member_id, product_id)：同一会员同一商品只有一条购物车记录，加购即更新数量。

### oms_order（订单）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_no | VARCHAR(64) | NOT NULL, UNIQUE | 订单号 |
| member_id | BIGINT | NOT NULL, INDEX | 会员ID |
| status | TINYINT | NOT NULL, DEFAULT 0, INDEX | 0-待付款 1-已付款 2-已发货 3-已完成 4-已关闭 |
| total_amount | DECIMAL(10,2) | NOT NULL | 订单总金额 |
| pay_amount | DECIMAL(10,2) | NOT NULL | 实付金额 |
| receiver_name | VARCHAR(64) | NOT NULL | 收货人 |
| receiver_phone | VARCHAR(20) | NOT NULL | 联系电话 |
| receiver_addr | VARCHAR(255) | NOT NULL | 收货地址 |
| note | VARCHAR(255) | | 买家备注 |
| express_company | VARCHAR(64) | | 物流公司（V5 新增） |
| express_no | VARCHAR(64) | | 物流单号（V5 新增） |
| admin_remark | VARCHAR(255) | | 后台备注（V5 新增） |
| pay_type | TINYINT | | 支付方式（0-模拟支付） |
| pay_time | DATETIME | | 支付时间 |
| ship_time | DATETIME | | 发货时间 |
| finish_time | DATETIME | | 完成时间 |
| close_time | DATETIME | | 关闭时间 |
| close_reason | VARCHAR(255) | | 关闭原因 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW | 创建时间 |
| update_time | DATETIME | NOT NULL, ON UPDATE NOW | 更新时间 |

> **订单状态机**：0→1（支付）→2（发货）→3（完成）；0→4/1→4（关单）。EnumMap 校验流转。

### oms_order_item（订单明细）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_id | BIGINT | NOT NULL, INDEX | 订单ID |
| order_no | VARCHAR(64) | NOT NULL, INDEX | 订单号 |
| product_id | BIGINT | NOT NULL | 商品ID |
| product_pic | VARCHAR(255) | | 商品图片（快照） |
| product_name | VARCHAR(200) | NOT NULL | 商品名称（快照） |
| price | DECIMAL(10,2) | NOT NULL | 成交单价 |
| quantity | INT | NOT NULL | 购买数量 |
| total_price | DECIMAL(10,2) | NOT NULL | 小计金额 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW | 创建时间 |

### oms_pay_log（支付回调日志）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| pay_no | VARCHAR(64) | NOT NULL, UNIQUE | 第三方支付流水号 |
| order_no | VARCHAR(64) | NOT NULL, INDEX | 订单号 |
| amount | DECIMAL(10,2) | NOT NULL | 支付金额 |
| success | TINYINT | NOT NULL, DEFAULT 1 | 0-失败 1-成功 |
| callback_body | TEXT | | 回调报文 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW | 回调时间 |

> **支付幂等**：pay_no 唯一约束 + 订单状态 CAS，同一支付流水号只会成功一次。

### ums_admin_log（管理操作审计日志）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| admin_id | BIGINT | NOT NULL, INDEX | 管理员ID |
| operation | VARCHAR(64) | NOT NULL | 操作权限编码 |
| method | VARCHAR(10) | | HTTP 方法 |
| path | VARCHAR(128) | | 请求路径 |
| params | TEXT | | 请求参数（JSON，敏感字段掩码） |
| ip | VARCHAR(64) | | 来源 IP |
| success | TINYINT | NOT NULL, DEFAULT 1 | 0-失败 1-成功 |
| error_msg | VARCHAR(500) | | 失败原因 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW, INDEX | 操作时间 |

### ums_member_log（会员业务操作日志）

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| member_id | BIGINT | NOT NULL, DEFAULT 0, INDEX | 会员ID（登录失败时为 0） |
| username | VARCHAR(64) | | 会员账号（冗余，便于追溯） |
| operation | VARCHAR(64) | NOT NULL | 操作类型（MEMBER_LOGIN/ORDER_CREATE 等） |
| method | VARCHAR(10) | | HTTP 方法 |
| path | VARCHAR(128) | | 请求路径 |
| params | TEXT | | 请求参数（JSON，敏感字段掩码） |
| ip | VARCHAR(64) | | 来源 IP |
| success | TINYINT | NOT NULL, DEFAULT 1 | 0-失败 1-成功 |
| error_msg | VARCHAR(500) | | 失败原因 |
| create_time | DATETIME | NOT NULL, DEFAULT NOW, INDEX | 操作时间 |

## Flyway 迁移文件

| 版本 | 文件 | 内容 |
|---|---|---|
| V1 | init_schema.sql | 11 张基础表（admin/member/category/brand/product/cart/order/order_item/pay_log） |
| V2 | seed_data.sql | 种子数据（4 分类/5 品牌/8 商品） |
| V3 | rbac.sql | RBAC 5 表 + 12 权限点 + 2 角色 |
| V4 | member_log.sql | 会员日志表 + 会员日志查看权限 |
| V5 | admin_mgmt_logistics.sql | 订单物流/备注字段 + 8 权限点（管理员/会员/订单增强） |
