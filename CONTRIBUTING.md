# 贡献指南

## Git 提交规范

本项目采用 **Conventional Commits** 约定式提交规范。

### 提交格式

```
<type>: <description>

<optional body>
```

### type 类型

| type | 说明 | 示例 |
|---|---|---|
| `feat` | 新功能 | `feat: 新增会员封禁功能` |
| `fix` | Bug 修复 | `fix: 修复订单金额计算错误` |
| `docs` | 文档变更 | `docs: 更新 API 文档` |
| `style` | 代码格式（不影响逻辑） | `style: 统一缩进为 4 空格` |
| `refactor` | 重构（非新功能、非修复） | `refactor: 提取公共分页逻辑` |
| `test` | 测试相关 | `test: 新增订单状态机测试` |
| `chore` | 构建/工具/依赖变更 | `chore: 升级 Spring Boot 3.3.5` |
| `security` | 安全修复 | `security: 移除硬编码数据库密码` |

### 规则

1. **一个提交只做一件事**：不要在 `feat: 新增登录功能` 中混入 `fix: 修复商品价格 bug`
2. **描述用中文**，简洁明了，不超过 50 字
3. **body 可选**：复杂变更补充原因和影响
4. **禁止** `--no-verify` 跳过 hook

### 示例

```
feat: 新增管理员管理 CRUD 接口

- 管理员分页（含角色名，不返回密码）
- 新增/编辑/重置密码/分配角色
- 禁止禁用自己
```

## 分支策略

| 分支 | 用途 |
|---|---|
| `main` | 生产分支，保持可部署状态 |
| `dev` | 开发分支（可选） |
| `feature/*` | 功能分支（如 feature/member-ban） |
| `fix/*` | 修复分支（如 fix/order-amount） |

## 开发流程

1. 从 `main` 拉取最新代码：`git pull origin main`
2. 创建功能分支：`git checkout -b feature/xxx`
3. 开发 + 测试
4. 提交：`git add <files> && git commit -m "feat: xxx"`
5. 推送：`git push origin feature/xxx`
6. 创建 PR 合并到 `main`

## 安全要求

- **禁止**提交 `.env` 文件（已被 .gitignore 排除）
- **禁止**在代码中硬编码密码/密钥/API Key
- **禁止**提交 `target/`、`node_modules/`、`*.jar` 等构建产物
- 敏感配置通过环境变量注入，参见 `.env.example`
