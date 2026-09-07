# 部署文档

## 一、服务器要求

| 项目 | 最低配置 | 推荐配置 |
|---|---|---|
| CPU | 2 核 | 4 核 |
| 内存 | 2 GB | 4 GB |
| 磁盘 | 20 GB | 50 GB SSD |
| JDK | 21+ | 21 |
| Docker | 24+ | 25+ |

## 二、环境变量列表

| 变量名 | 必填(prod) | 默认值(dev) | 说明 |
|---|---|---|---|
| MYSQL_HOST | 否 | localhost | MySQL 主机 |
| MYSQL_PORT | 否 | 3306 | MySQL 端口 |
| MYSQL_DB | 否 | cloude_shop | 数据库名 |
| MYSQL_USER | 否 | root | 数据库用户 |
| MYSQL_PASSWORD | **是** | 无 | 数据库密码 |
| REDIS_HOST | 否 | localhost | Redis 主机 |
| REDIS_PORT | 否 | 6379 | Redis 端口 |
| REDIS_PASSWORD | 否 | 空 | Redis 密码 |
| RABBITMQ_HOST | 否 | localhost | RabbitMQ 主机 |
| RABBITMQ_PORT | 否 | 5672 | RabbitMQ 端口 |
| RABBITMQ_USER | 否 | guest | RabbitMQ 用户 |
| RABBITMQ_PASSWORD | 否 | guest | RabbitMQ 密码 |
| JWT_SECRET | **是** | 无 | JWT 签名密钥（>= 32 字节，`openssl rand -base64 48`） |
| ADMIN_INIT_PASSWORD | 否 | admin123 | 首次初始化超管密码 |
| CORS_ORIGINS | **是** | localhost | 前端域名（逗号分隔） |

## 三、环境分离

| Profile | 命令 | 特点 |
|---|---|---|
| dev | `--spring.profiles.active=dev` | 允许默认密码、开启 Swagger UI |
| prod | `--spring.profiles.active=prod` | 关闭 Swagger、JWT_SECRET 必须注入（dev 密钥拒绝启动） |

## 四、部署步骤

### 4.1 获取代码

```bash
git clone https://github.com/flyflyylgy/aishop.git
cd aishop
```

### 4.2 启动基础设施

```bash
docker compose up -d
# 验证：docker ps 检查 mysql/redis/rabbitmq 状态
```

### 4.3 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填入实际密码和 JWT_SECRET
source .env  # Linux
# Windows PowerShell: 逐行 $env:KEY='VALUE'
```

### 4.4 编译打包

```bash
mvn clean package -DskipTests
```

### 4.5 启动后端（生产模式）

```bash
# 先启动 portal（执行数据库迁移）
java -jar shop-portal/target/shop-portal-1.0.0.jar \
  --spring.profiles.active=prod \
  --spring.profiles.active=prod

# 启动 admin
java -jar shop-admin/target/shop-admin-1.0.0.jar \
  --spring.profiles.active=prod
```

### 4.6 构建前端

```bash
# PC 商城
cd shop-web-portal && npm install && npm run build
# 管理后台
cd shop-web-admin && npm install && npm run build
# H5 移动端
cd shop-web-h5 && npm install && npm run build
```

### 4.7 Nginx 反向代理（生产）

```nginx
# PC 商城
server {
    listen 80;
    server_name shop.example.com;
    root /opt/aishop/portal/dist;
    location / { try_files $uri $uri/ /index.html; }
    location /portal-api/ { proxy_pass http://127.0.0.1:8080/; }
}

# 管理后台
server {
    listen 80;
    server_name admin.example.com;
    root /opt/aishop/admin/dist;
    location / { try_files $uri $uri/ /index.html; }
    location /admin-api/ { proxy_pass http://127.0.0.1:8081/; }
}
```

## 五、Docker 一键部署（可选）

```bash
# 构建镜像
docker build -t aishop-portal -f Dockerfile.portal .
docker build -t aishop-admin -f Dockerfile.admin .

# 运行
docker run -d --name portal --env-file .env -p 8080:8080 aishop-portal
docker run -d --name admin --env-file .env -p 8081:8081 aishop-admin
```

## 六、验证

```bash
# 验证后端
curl http://localhost:8081/admin-api/admin/captcha
# 返回 {"code":200,"data":{"captchaKey":"...","img":"data:image/png;base64,..."}}

# 验证前端
curl http://localhost/  # 应返回 HTML
```

## 七、回滚

```bash
# 查看提交历史
git log --oneline

# 回滚到指定版本
git checkout <commit-hash>
mvn clean package -DskipTests
# 重新启动
```
