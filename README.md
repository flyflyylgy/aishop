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

### 3. 环境变量配置（安全要求）

复制 `.env.example` 为 `.env` 并填入实际值。`.env` 已被 `.gitignore` 排除，不会提交到仓库。

```bash
cp .env.example .env
# 编辑 .env 填入 MySQL 密码、JWT 密钥等
```

**环境分离**：

| Profile | 命令 | 说明 |
|---|---|---|
| dev（默认） | `java -jar xxx.jar --spring.profiles.active=dev` | 允许开发默认密码、开启 Swagger |
| prod | `java -jar xxx.jar --spring.profiles.active=prod` | 关闭 Swagger，JWT_SECRET 必须注入（dev 密钥会拒绝启动） |

> 生产环境启动前必须设置：`JWT_SECRET`（>= 32 字节随机串）、`MYSQL_PASSWORD`、`CORS_ORIGINS`（真实前端域名）。生成密钥：`openssl rand -base64 48`

### 4. 启动服务（先启动 portal，由它执行数据库迁移）

```bash
# Windows PowerShell
$env:MYSQL_PORT='3307'
$env:MYSQL_PASSWORD='root123456'       # 同 docker-compose 中设置的密码
$env:JWT_SECRET='dev-only-please-change-me-in-production-0123456789'
java -jar shop-portal/target/shop-portal-1.0.0.jar --spring.profiles.active=dev
# 另开窗口
$env:MYSQL_PORT='3307'
$env:MYSQL_PASSWORD='root123456'
$env:JWT_SECRET='dev-only-please-change-me-in-production-0123456789'
java -jar shop-admin/target/shop-admin-1.0.0.jar --spring.profiles.active=dev

# Linux / macOS
MYSQL_PORT=3307 MYSQL_PASSWORD=root123456 \
JWT_SECRET=dev-only-please-change-me-in-production-0123456789 \
java -jar shop-portal/target/shop-portal-1.0.0.jar --spring.profiles.active=dev
```

可覆盖的环境变量：`MYSQL_HOST/MYSQL_PORT/MYSQL_DB/MYSQL_USER/MYSQL_PASSWORD`、`REDIS_HOST/REDIS_PORT/REDIS_PASSWORD`、`RABBITMQ_HOST/RABBITMQ_PORT/RABBITMQ_USER/RABBITMQ_PASSWORD`、`JWT_SECRET`、`ADMIN_INIT_PASSWORD`、`CORS_ORIGINS`。

数据库 `cloude_shop`、表结构、种子数据（4 分类/5 品牌/8 商品）由 Flyway 自动创建。
首次启动后台会自动初始化管理员：**admin**（密码来自 `ADMIN_INIT_PASSWORD` 环境变量，未设置则默认 `admin123`）。

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

## 模块化设计（Modular Design）

本项目完整实现了四项核心模块化设计原则：

### 1. 全局异常处理（Global Exception Handling）

> 一个集中的"异常捕获站"，所有模块抛出的错误都流向这里统一处理。

**实现**：[GlobalExceptionHandler.java](shop-common/src/main/java/com/cloude/shop/common/exception/GlobalExceptionHandler.java) — `@RestControllerAdvice` 统一异常处理器

| 异常类型 | 处理方式 | HTTP 状态 |
|---|---|---|
| `BusinessException` | 业务异常，返回错误消息 | 200 + code=400 |
| `MethodArgumentNotValidException` | 参数校验失败 | 200 + code=400 |
| `HttpMessageNotReadableException` | JSON 格式错误 | 200 + code=400 |
| `DuplicateKeyException` | 唯一约束冲突（重复提交） | 200 + code=400 |
| `NoHandlerFoundException` / `NoResourceFoundException` | 接口不存在 | 200 + code=404 |
| `Exception`（兜底） | 未知系统异常 | 200 + code=500 |

所有异常统一返回 `CommonResult` 结构，前端只需解析 `body.code` 判断业务结果。

### 2. AOP 面向切面编程（Aspect-Oriented Programming）

> 在不修改业务代码的情况下，统一处理所有模块的通用逻辑（如日志、鉴权、异常捕获）。

**实现**：双审计日志切面

| 切面 | 文件 | 切点 | 通知类型 | 功能 |
|---|---|---|---|---|
| 管理端审计 | [AdminLogAspect.java](shop-admin/src/main/java/com/cloude/shop/admin/aspect/AdminLogAspect.java) | `@RequirePermission` + `@OpLog` 注解方法 | `@Around` | 记录管理员操作（登录/改密/CRUD/发货/授权等），含 IP/参数/成败/错误原因，敏感字段自动掩码 |
| 会员端审计 | [MemberLogAspect.java](shop-portal/src/main/java/com/cloude/shop/portal/aspect/MemberLogAspect.java) | `@OpLog` 注解方法 | `@Around` | 记录会员操作（注册/登录/下单/支付/取消/收货/购物车） |

**实现**：三重拦截器链

| 拦截器 | 文件 | 功能 |
|---|---|---|
| `RateLimitInterceptor` | [RateLimitInterceptor.java](shop-common/src/main/java/com/cloude/shop/common/component/RateLimitInterceptor.java) | Redis 固定窗口限流（敏感接口 60s/10 次，普通 10s/100 次，超限 429） |
| `JwtInterceptor` | [JwtInterceptor.java](shop-common/src/main/java/com/cloude/shop/common/component/JwtInterceptor.java) | JWT 签名校验 + Redis 登录态检查 + UserContext 填充 |
| `AdminPermissionInterceptor` | [AdminPermissionInterceptor.java](shop-admin/src/main/java/com/cloude/shop/admin/config/AdminPermissionInterceptor.java) | RBAC 权限检查（`@RequirePermission` 注解匹配） |

拦截器在 [WebMvcConfig](shop-admin/src/main/java/com/cloude/shop/admin/config/WebMvcConfig.java) 中按顺序注册，业务代码零侵入。

### 3. 代码复用 / DRY 原则（Don't Repeat Yourself）

> 避免重复代码，提高维护性。

**shop-common 公共模块**（所有子模块共享）：

| 公共类 | 文件 | 复用场景 |
|---|---|---|
| `CommonResult<T>` | [CommonResult.java](shop-common/src/main/java/com/cloude/shop/common/api/CommonResult.java) | 统一 API 响应封装（success/failed/forbidden/unauthorized） |
| `ResultCode` | [ResultCode.java](shop-common/src/main/java/com/cloude/shop/common/api/ResultCode.java) | 统一业务状态码枚举（200/400/401/403/404/500/600/601/602） |
| `BusinessException` | [BusinessException.java](shop-common/src/main/java/com/cloude/shop/common/exception/BusinessException.java) | 统一业务异常，携带 code+message |
| `JwtInterceptor` | [JwtInterceptor.java](shop-common/src/main/java/com/cloude/shop/common/component/JwtInterceptor.java) | portal/admin 共用 JWT 校验逻辑 |
| `RateLimitInterceptor` | [RateLimitInterceptor.java](shop-common/src/main/java/com/cloude/shop/common/component/RateLimitInterceptor.java) | portal/admin 共用限流逻辑 |
| `UserContext` | [UserContext.java](shop-common/src/main/java/com/cloude/shop/common/component/UserContext.java) | ThreadLocal 当前用户上下文，避免参数传递 |
| `JwtUtil` | [JwtUtil.java](shop-common/src/main/java/com/cloude/shop/common/util/JwtUtil.java) | JWT 签发/解析/校验工具 |
| `SensitiveLogUtil` | [SensitiveLogUtil.java](shop-common/src/main/java/com/cloude/shop/common/util/SensitiveLogUtil.java) | 日志脱敏工具（password/token 自动掩码 ******） |
| `@RequirePermission` | [RequirePermission.java](shop-common/src/main/java/com/cloude/shop/common/annotation/RequirePermission.java) | 权限注解，一行代码声明接口所需权限 |
| `@OpLog` | [OpLog.java](shop-common/src/main/java/com/cloude/shop/common/annotation/OpLog.java) | 审计注解，一行代码声明操作名称 |

### 4. 关注点分离（Separation of Concerns, SoC）

> 业务逻辑（做什么）和系统逻辑（如日志、异常）分开管理，各司其职。

**Maven 多模块拆分**（根 [pom.xml](pom.xml)）：

| 模块 | 职责 | 依赖 |
|---|---|---|
| `shop-common` | 系统逻辑：异常处理、拦截器、工具类、注解、公共 API | 无 |
| `shop-mapper` | 数据访问：Entity、Mapper 接口、Flyway 迁移 | shop-common |
| `shop-service` | 业务逻辑：Service、DTO、组件（验证码/锁定/RBAC） | shop-mapper |
| `shop-portal` | 前台入口：Controller、AOP 切面、配置 | shop-service |
| `shop-admin` | 后台入口：Controller、AOP 切面、配置 | shop-service |

**shop-service 内部分层**：

| 目录 | 职责 | 示例文件 |
|---|---|---|
| `service/` | 核心业务逻辑 | OrderService、MemberService、AdminService、RbacService |
| `dto/` | 数据传输对象 | OrderCreateParam、TokenVO、OrderDetailVO |
| `component/` | 可复用业务组件 | CaptchaService、LoginGuard |
| `config/` | 业务配置 | RedisKeepAliveConfig |

**分离效果**：
- Controller 只管接收请求 → 调 Service → 返回 `CommonResult`（不处理异常/不写日志）
- Service 只管业务逻辑（不处理 JWT/权限/限流/日志）
- 异常处理 → GlobalExceptionHandler 集中捕获
- 操作日志 → AOP 切面自动记录
- 权限校验 → 拦截器前置检查
- 限流 → 拦截器前置检查

业务代码中 **零行** try-catch、零行 日志记录、零行 权限判断——全部由系统层统一处理。

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

## 安全审查与版本控制流程

> 每次更新功能后按此流程执行：

1. **安全检查**：扫描代码中是否有硬编码密码/密钥/API Key，确保敏感值通过环境变量注入
2. **更新功能清单**：在上方「功能完成清单」表中新增条目
3. **编译验证**：`mvn clean package -DskipTests` + `node vite build` 三端前端
4. **提交代码**：`git add -A && git commit -m "feat: xxx功能描述"`
5. **推送到 GitHub**：`git push origin main`

### 环境配置安全规范

| 规范 | 说明 |
|---|---|
| `.env` 文件 | 存放实际密码/密钥，已被 `.gitignore` 排除，**不提交到仓库** |
| `.env.example` 文件 | 环境变量模板（无实际值），提交到仓库供参考 |
| `application-dev.yml` | 开发环境配置（允许默认密码、开启 Swagger） |
| `application-prod.yml` | 生产环境配置（关闭 Swagger、JWT_SECRET 必须注入） |
| `JwtConfig` 启动校验 | 生产 profile 检测到 dev 密钥直接拒绝启动 |
| 密码加密 | 所有密码使用 BCrypt 哈希存储，源码中不保存明文 |
| 日志脱敏 | 审计日志自动掩码 password/token 等敏感字段 |
