# Cloude Shop — 企业级 Java 购物商城

参考 GitHub 主流开源商城（[mall](https://github.com/macrozheng/mall) 83.7k★、[litemall](https://github.com/linlinjava/litemall) 20.3k★、[newbee-mall](https://github.com/newbee-ltd/newbee-mall) 11.6k★、[EasyMall](https://github.com/yunluoxincheng/EasyMall)）的最佳实践，构建的单体多模块企业级电商后端。

## 技术栈

| 组件 | 版本 | 用途 |
|---|---|---|
| JDK | 21 | 运行时 |
| Spring Boot | 3.3.5 | 基础框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7 | 缓存（商品/分类/登录态） |
| RabbitMQ | 3 | 延迟关单（TTL + DLX） |
| MyBatis Plus | 3.5.7 | ORM |
| JWT (jjwt) | 0.12.6 | 认证 + Redis 可吊销登录态 |
| Flyway | - | 数据库版本迁移 |
| SpringDoc | 2.6.0 | Swagger 接口文档 |

## 模块结构

```
cloude-shop
├── shop-common      通用模块：统一响应/全局异常/JWT/拦截器/订单状态枚举
├── shop-mapper      实体 + Mapper（含库存三态 CAS、订单状态 CAS SQL）+ Flyway 脚本
├── shop-service     核心业务：订单状态机/库存服务/支付幂等/MQ/购物车/商品
├── shop-admin       后台管理服务（演示端口 9301）/admin-api
└── shop-portal      前台商城服务（演示端口 9300）/portal-api（含 Flyway 迁移）
```

## 企业级核心设计

| 设计 | 实现 |
|---|---|
| **订单状态机** | `OrderStatus` 基于 `EnumMap` 转换表统一校验流转（待付款→已付款→已发货→已完成 / 已关闭） |
| **库存三态分离** | `available_stock / locked_stock / sold_stock`，下单 `available→locked`、支付 `locked→sold`、关单归还，条件更新（CAS）防超卖 |
| **支付幂等** | 回调日志 `pay_no` 唯一约束 + 订单状态 CAS（`PENDING_PAYMENT→PAID` 仅一次成功）+ 金额核对 |
| **MQ 延迟关单** | RabbitMQ TTL + DLX：下单后 30 分钟未支付，死信转发自动关单并释放库存，事务提交后才发消息 |
| **登录态安全** | JWT 签名 + Redis 存储可吊销 token，BCrypt 密码加密 |
| **RBAC 权限** | 角色/权限点/关联表（V3 迁移），`@RequirePermission` 注解 + 拦截器校验，权限集 Redis 缓存 10 分钟；`SUPER_ADMIN` 通配，`OPERATOR` 最小授权；管理员登录返回 `perms` 权限码集合，前端菜单/按钮按权限码显隐 |
| **操作审计** | 双审计体系：`@OpLog` 注解 + AOP 切面。管理员侧 `ums_admin_log`（登录/登出/改密/全部权限接口）、会员侧 `ums_member_log`（注册/登录/下单/支付/取消/收货/购物车），均记录操作人、时间、IP、方法、路径、参数（password/token 等敏感字段自动掩码 `******`）、成败与失败原因；切面异常不影响主流程 |
| **接口限流** | Redis 固定窗口：敏感接口（登录/注册/支付回调）每 IP 60s/10 次，其余 10s/100 次，超限 429 |
| **缓存穿透防护** | 商品详情空值缓存 60s；权限集/分类树空结果同样缓存 |
| **CORS 配置化** | `shop.cors.allowed-origins` 环境变量注入 |
| **图形验证码** | JDK AWT 自绘 4 位验证码（去除易混淆字符 0/O/1/I），Redis 存储 5 分钟，一次性消费防枚举；管理端/会员端各自暴露 captcha 接口 |
| **账号锁定** | 账号级失败锁定：同账号连续失败 5 次锁 15 分钟，Redis 计数 + TTL，key 按角色区分（admin/member） |
| **密码强度校验** | 注册/改密要求 8-32 位且同时包含字母和数字，前后端双重校验 |
| **管理员管理闭环** | 管理员 CRUD、重置密码、分配角色（全量覆盖 + 删权限缓存）、角色权限勾选保存（实时生效）、权限点列表 |
| **会员管理增强** | 会员分页（含订单数/累计消费统计）、封禁/解封（封禁即吊销 token） |
| **订单管理增强** | 订单详情抽屉（物流信息/明细/后台备注）、订单号/收货人/电话关键词筛选、发货登记物流公司+运单号、后台订单备注 |

## 快速启动

### 1. 启动中间件（需 Docker）

```bash
docker compose up -d
```

### 2. 编译

```bash
mvn clean package -DskipTests
```

### 3. 启动服务（先启动 portal，由它执行数据库迁移）

本机 Docker 映射 MySQL 到 **3307**（避开本机已占用的 3306），yml 默认值 3306，因此需通过环境变量覆盖端口：

```bash
# Windows PowerShell（端口选 9300/9301：避开 Windows Hyper-V 动态排除区，
# 该区间每次重启会漂移，若绑定报"port already in use"请用
# netsh interface ipv4 show excludedportrange protocol=tcp 查看后换端口）
$env:MYSQL_PORT='3307'
$env:SERVER_PORT='9300'; java -jar shop-portal/target/shop-portal-1.0.0.jar
# 另开窗口
$env:MYSQL_PORT='3307'
$env:SERVER_PORT='9301'; java -jar shop-admin/target/shop-admin-1.0.0.jar

# Linux / macOS
MYSQL_PORT=3307 SERVER_PORT=9300 java -jar shop-portal/target/shop-portal-1.0.0.jar
MYSQL_PORT=3307 SERVER_PORT=9301 java -jar shop-admin/target/shop-admin-1.0.0.jar
```

可覆盖的环境变量：`MYSQL_HOST/MYSQL_PORT/MYSQL_DB/MYSQL_USER/MYSQL_PASSWORD`、`REDIS_HOST/REDIS_PORT`、`RABBITMQ_HOST/RABBITMQ_PORT`、`SERVER_PORT`、`JWT_SECRET`。

数据库 `cloude_shop`、表结构、种子数据（4 分类/5 品牌/8 商品）由 Flyway 自动创建。
首次启动后台会自动初始化管理员：**admin / admin123**。

> Redis 连接已内置 TCP keepalive（空闲 60s 后内核 15s 探测）+ 连接池 30s 驱逐，
> 长时间闲置后 Docker/WSL 回收连接不会再导致接口报
> `Unable to write command into connection`。

### 4. 启动前端（Vue 3 + Vite + Element Plus）

```bash
# 商城前台（http://localhost:5173，代理 /portal-api → 9300）
cd shop-web-portal && npm install && npm run dev

# 商城 H5 移动端（http://localhost:5175，代理 /portal-api → 9300）
cd shop-web-h5 && npm install && npm run dev

# 管理后台（http://localhost:5174，代理 /admin-api → 9301）
cd shop-web-admin && npm install && npm run dev
```

- 前台功能：注册登录、首页（分类导航/搜索/热销榜）、商品详情、购物车（勾选/数量/清空）、结算下单（自动生成幂等键）、订单列表（状态筛选）、模拟支付闭环、确认收货、超时自动关单演示。
- H5 功能：底部 Tabbar（首页/分类/购物车/我的）、轮播 Banner、分类宫格与侧边分类页、商品详情 GoodsAction 数量弹层、购物车滑动删除、结算/订单/模拟支付，复用 portal 全部 API。
- 后台功能：仪表盘统计、商品管理（CRUD/上下架）、分类管理、订单管理（发货/详情/筛选/物流单号/备注）、会员管理（分页/封禁解封）、管理员管理（CRUD/重置密码/分配角色）、角色权限配置（勾选权限点保存）、管理审计日志、会员操作日志、修改密码。
- 后端端口不同时（如演示用 9300/9301），同步修改各 `vite.config.js` 的 proxy 目标即可（注意用 `127.0.0.1` 而非 `localhost`，避免 Node 17+ IPv6 解析问题）；生产构建 `npm run build` 产物在 `dist/`。
- 演示账号：前台 `smoke01 / 123456`（或自行注册），后台 `admin / admin123`。

### 5. 接口文档

- 前台：http://localhost:9300/portal-api/doc.html
- 后台：http://localhost:9301/admin-api/doc.html

## 核心接口清单

### 前台 /portal-api（无需登录）
| 接口 | 说明 |
|---|---|
| POST `/member/register` / `/member/login` | 注册/登录（需验证码） |
| GET `/member/captcha` | 图形验证码 |
| GET `/product/{id}` / `/product/page` / `/product/hot` | 商品详情/搜索/热销 |
| GET `/product/category_tree` | 分类树 |

### 前台 /portal-api（需登录，Header: `Authorization: Bearer <token>`）
| 接口 | 说明 |
|---|---|
| GET/POST `/cart/**` | 购物车增删改查/勾选 |
| POST `/order/create` | 下单（锁库存 + 30min 延迟关单） |
| POST `/pay/create/{orderNo}` | 创建支付单 |
| POST `/pay/notify` | 支付回调（幂等） |
| POST `/order/cancel/{orderNo}` | 取消订单（释放库存） |
| POST `/order/confirm/{orderId}` | 确认收货 |

### 后台 /admin-api（需管理员登录）
| 接口 | 说明 |
|---|---|
| GET `/admin/captcha` | 管理端图形验证码 |
| POST `/admin/login` | 登录（admin/admin123，需验证码，返回 perms 权限码） |
| GET `/product/page` | 商品分页 |
| POST `/product/create` / `/product/update/{id}` / `/product/status/{id}/{status}` | 商品管理 |
| GET `/order/page` + GET `/order/{id}` | 订单分页（keyword 筛选）/ 订单详情 |
| POST `/order/ship/{orderId}` | 发货（物流公司+运单号） |
| POST `/order/remark/{orderId}` | 后台订单备注 |
| GET `/admin-user/page` | 管理员分页（需 `shop:admin:list`） |
| POST `/admin-user/create` / `/admin-user/update/{id}` / `/admin-user/reset-password/{id}` | 管理员 CRUD |
| POST `/admin-user/roles/{id}` | 分配角色（需 `shop:admin:assign`） |
| GET `/member/page` | 会员分页（含订单数/消费额，需 `shop:member:list`） |
| POST `/member/status/{id}/{status}` | 封禁/解封会员（封禁即吊销 token） |
| GET `/permission/list` | 全部权限点（授权勾选用） |
| GET `/role/permissions` + POST `/role/permissions` | 查询/保存角色权限（实时生效） |
| GET `/role/list` + GET `/log/page` | 角色列表 / 管理操作审计日志（需 `shop:rbac:list`） |
| GET `/member-log/page` | 会员业务操作日志（注册/登录/下单/支付等，需 `shop:log:member`，仅超管） |

## 全流程验证（curl 示例）

```bash
# 1. 注册登录
curl -X POST http://localhost:9300/portal-api/member/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test01","password":"123456","nickname":"测试用户"}'
TOKEN=$(curl -s -X POST http://localhost:9300/portal-api/member/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test01","password":"123456"}' | jq -r .data.token)

# 2. 下单（Idempotency-Key 由客户端生成 UUID，10 分钟内防重复提交）
curl -X POST http://localhost:9300/portal-api/order/create \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"items":[{"productId":1,"quantity":2}],"receiverName":"张三","receiverPhone":"13800138000","receiverAddr":"上海市浦东新区xx路1号"}'

# 商品浏览：列表 GET /portal-api/product/page?keyword=&pageNum=1&pageSize=10
#           详情 GET /portal-api/product/1    热销 GET /portal-api/product/hot?limit=6

# 3. 创建支付单 → 模拟回调（幂等）
curl -X POST http://localhost:9300/portal-api/pay/create/ORDER_NO \
  -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:9300/portal-api/pay/notify \
  -H "Content-Type: application/json" \
  -d '{"payNo":"PAY001","orderNo":"ORDER_NO","amount":9998.00,"success":true}'
```

## 生产化扩展路线

- **安全基线（已内置）**：JWT 密钥通过环境变量 `JWT_SECRET` 注入（>= 32 字节，缺失拒绝启动）；下单幂等 `Idempotency-Key`；MQ 投递 confirm + 超时关单兜底任务；生产 profile 关闭 Swagger（`--spring.profiles.active=prod`）；首次登录后立即调用 `POST /admin-api/admin/password` 更换默认口令
- **搜索**：商品搜索接入 Elasticsearch（当前为 MySQL LIKE）
- **支付**：对接微信/支付宝官方 SDK（替换模拟通道），增加主动查单对账
- **限流**：网关/接口层接入 Sentinel
- **秒杀**：Redis 预扣库存 + MQ 削峰
- **微服务化**：参考 mall 的 mall-portal/mall-admin 拆分方式拆出独立部署单元
- **可观测**：接入 SkyWalking / Prometheus + Grafana

## 功能完成清单

> 每次新增功能后同步更新此表，提交 Git 并推送到远程仓库。

### 基础电商（已完成）

| 功能 | 说明 |
|---|---|
| 会员注册/登录 | BCrypt 密码加密，JWT + Redis 可吊销 token |
| 商品浏览 | 首页/分类导航/搜索/热销榜/详情页，缓存穿透防护 |
| 购物车 | 增删改查/勾选/清空 |
| 订单状态机 | 待付款→已付款→已发货→已完成/已关闭，EnumMap 校验 |
| 库存三态 CAS | available/locked/sold，下单锁/支付扣/关单还，防超卖 |
| 支付幂等 | pay_no 唯一约束 + 订单 CAS + 金额核对 |
| MQ 延迟关单 | RabbitMQ TTL+DLX，30 分钟未支付自动关单 |
| 下单幂等 | Idempotency-Key 防重复提交 |
| 接口限流 | Redis 固定窗口，敏感接口 60s/10 次 |
| 操作审计 | 双审计（管理员+会员），AOP 自动记录，敏感字段掩码 |
| RBAC 权限 | 角色/权限点/关联表，@RequirePermission + 拦截器，超管通配 |
| 全局异常处理 | BusinessException/参数校验/JSON 解析/重复键/404/兜底 |
| Flyway 迁移 | V1~V5 自动建表+种子数据 |
| PC 商城前端 | Vue3+Element Plus，完整购物流程 |
| H5 移动端 | Vue3+Vant，Tabbar/轮播/分类/购物车/订单 |
| 管理后台前端 | Vue3+Element Plus，商品/分类/订单/角色/日志 |

### 安全加固（已完成）

| 功能 | 说明 |
|---|---|
| 图形验证码 | JDK AWT 自绘 4 位，Redis 5 分钟，一次性消费，管理端+会员端 |
| 账号锁定 | 同账号失败 5 次锁 15 分钟，Redis 计数+TTL，按角色区分 |
| 密码强度校验 | 8-32 位含字母+数字，前后端双重校验 |

### 权限管理闭环（已完成）

| 功能 | 说明 |
|---|---|
| 管理员 CRUD | 新增（重名+强度检查）/编辑（禁止禁用自己）/重置密码 |
| 分配角色 | 全量覆盖 ums_admin_role + 删权限缓存 |
| 角色权限配置 | 勾选权限点保存，全量覆盖 + evictCacheByRole 实时生效 |
| 权限点列表 | 21 个权限点，按 ID 排序 |
| 前端权限显隐 | 登录返回 perms 存 localStorage，菜单 v-if perm、按钮 v-perms |

### 会员+订单后台增强（已完成）

| 功能 | 说明 |
|---|---|
| 会员分页 | 含订单数/累计消费额统计 |
| 会员封禁/解封 | 封禁即遍历 Redis 删除该会员 token，吊销登录态 |
| 订单详情 | 物流信息/商品明细/后台备注 |
| 订单关键词筛选 | 订单号/收货人/电话模糊匹配 |
| 发货物流登记 | 物流公司（下拉选择）+ 运单号 |
| 后台订单备注 | 仅后台可见，不影响买家视图 |
