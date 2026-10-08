# XX教育学习中心

面向家长和学生的教育学习资源平台。当前仓库完成到 Sprint 02：分类、内容、文章和视频。

原有说明文件 `readme` 保持不变。

## 技术栈

- Java 21
- Spring Boot 3.5.16
- Maven
- MyBatis-Plus 3.5.17
- MySQL 8
- Redis
- Lombok、MapStruct、Hibernate Validator
- springdoc-openapi 2.8.17
- Flyway
- JWT（jjwt 0.12.6）

架构是单个 Maven 模块的模块化单体。业务按包划分，不拆微服务。

## 当前范围

已完成：统一响应、全局异常、参数校验、审计字段、逻辑删除、分页上限、Swagger、请求日志、Spring Security、JWT 解析与签发、Flyway 系统表，以及分类、内容目录、文章、视频的管理端和公开查询。

未实现：支付、会员、直播、AI、积分、排行榜，以及题目、专题、每周一题、资料的独立业务。登录接口未做。内容类型枚举已预留 QUESTION、TOPIC、WEEKLY、DOCUMENT。

## 本地依赖

MySQL 8 与 Redis。可以使用：

```bash
docker compose up -d
```

数据库由应用启动时的 Flyway 执行 `V1__init.sql` 和 `V2__create_content_tables.sql`。不要再手工执行第二套建表脚本。

默认连接见 `application-dev.yml`。本机账号口令通过环境变量覆盖：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `JWT_SECRET`：至少 32 字节。未设置时开发配置使用本地占位值，不能用于生产
- `WORKER_ID`、`DATACENTER_ID`：雪花算法机器号，单实例保持默认 1。多实例部署前必须改成不重复的 0 到 31。

## 启动

```bash
mvnw.cmd spring-boot:run
```

生产部署（腾讯云 COS + Docker Compose）见 [docs/deploy-prod.md](docs/deploy-prod.md)。

- 健康检查：`GET http://localhost:8080/api/health`
- Swagger UI：`http://localhost:8080/swagger-ui.html`

`/api/health`、`/api/v1/public/**`、分类/内容/文章/视频的公开 GET，以及 Swagger 可匿名访问。`/api/v1/admin/**` 和 `/admin/api/**` 需要 `Authorization: Bearer <JWT>`，写操作还要对应权限码。登录接口尚未实现，令牌由 `JwtTokenService` 签发。

## 测试

```bash
mvnw.cmd test
```

测试使用 H2，不连接本机 MySQL。H2 只存在于测试 classpath。

## 响应约定

HTTP 状态与 Body 中的 `code` 同时返回。`code` 是字符串。业务成功时 HTTP 200 且 `code` 为 `"0"`，`message` 为 `success`。失败时 `code` 与 HTTP 状态一致，例如 `"401"`、`"403"`、`"404"`、`"500"`。字段错误放在 `data`。雪花 Long 序列化为字符串。`X-Trace-Id` 只放在响应头。
