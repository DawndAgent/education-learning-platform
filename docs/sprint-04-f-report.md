# Sprint 04-F 完成报告

图片上传已经接到 Admin 编辑流程。文章封面、正文插图、视频封面和二维码都可以选文件上传，后端落到本地存储并返回可访问 URL。没有接入 COS / OSS / S3 SDK，也没有改小程序。

## 1. 完成内容

### Storage

- `StorageService` 抽象：`upload` / `delete` / `getUrl`
- `LocalStorageService`：写入 `./data/uploads`，返回 `/uploads/...`
- `ObjectStorageService`：预留实现，`storage.type` 为 `cos` / `s3` / `oss` 时选用，方法会明确抛出未配置异常
- 配置开关：`storage.type=local`（本 Sprint 可用）

### Upload API

- `POST /admin/api/files/upload`
- `multipart/form-data`，字段 `file`，可选 `scene=ARTICLE|COVER|VIDEO|QRCODE`
- 权限：`FILE_UPLOAD`
- 只允许 JPEG / PNG / WEBP / GIF，单文件最大 10MB
- 校验扩展名、声明的 Content-Type、Apache Tika 文件头
- objectKey 使用 `images/{scene}/yyyy/MM/dd/{uuid}.ext`，忽略用户原始路径

### Local Storage

- 目录：`data/uploads`
- 访问：`GET /uploads/**`（匿名可读）
- 不写 `src/main/resources`
- Docker Compose 已挂载：`./data/uploads:/app/data/uploads`

### ImageUpload + 编辑器

- `admin-web/src/components/ImageUpload.vue`：上传、预览、删除引用、进度
- 文章封面、视频封面、视频二维码复用该组件
- wangEditor 通过 `customUpload` 调用同一上传接口，插入返回的 URL，禁止把 Base64 写进正文

### Article / Video / Content

- 上传成功后把 URL 写入已有字段：`coverUrl`、`body` 中的 `<img src>`、`qrCodeUrl`
- 二维码和封面支持相对路径 `/uploads/...`；视频播放地址仍要求绝对 `http(s)` URL
- HTML 清洗保留相对上传路径，同时继续过滤脚本和事件属性

## 2. API

```text
POST /admin/api/files/upload
```

Request：

```text
Content-Type: multipart/form-data
file: <image>
scene: ARTICLE | COVER | VIDEO | QRCODE   # 可选，默认 COVER
Authorization: Bearer <token>
```

Response：

```json
{
  "code": "0",
  "data": {
    "url": "/uploads/images/covers/2026/09/24/xxx.png",
    "objectKey": "images/covers/2026/09/24/xxx.png",
    "fileName": "live.png",
    "contentType": "image/png",
    "size": 70
  }
}
```

Permission：`FILE_UPLOAD`（无权限 403）

Validation：

- 空文件 / 0 字节拒绝
- 超过 10MB 拒绝
- 非白名单扩展名拒绝
- Content-Type 与扩展名不一致拒绝
- Magic Number / Tika 检测不是图片拒绝
- SVG、txt、html、js、exe、zip、pdf 拒绝
- 伪装成 `image/png` 的 HTML 拒绝
- `../../x.png` 不会写出 uploads 根目录

## 3. 配置

```yaml
storage:
  type: local
  local:
    base-path: ./data/uploads
    public-url-prefix: /uploads

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 12MB
```

测试环境使用 `./target/test-uploads`，避免污染开发目录。

## 4. 权限

Flyway `V4__add_file_upload_permission.sql`：

- 新增权限 `FILE_UPLOAD`（id 109）
- 赋给管理员角色 `ADMIN`
- 登录后权限数量从 8 变为 9

后端继续用 `@RequirePermission(FILE_UPLOAD)`，没有写死用户名。

## 5. 数据库

```text
新增 migration：V4__add_file_upload_permission.sql
No file asset table introduced.
```

没有新增业务资源表。上传结果只保存 URL 到现有 Content / Article / Video 字段。

## 6. 测试

```text
mvn test
Tests run: 79, Failures: 0, Errors: 0, Skipped: 0

mvn verify
PASS（含 Checkstyle、SpotBugs）

npm run build
PASS

npx tsc --noEmit
PASS

npx tsc -p tsconfig.app.json --noEmit
PASS

admin-web node:test
20 pass / 0 fail
```

## 7. 安全测试

| 项 | 结果 |
| --- | --- |
| 文件类型白名单 | PNG/JPEG/GIF/WEBP 通过；txt/html/js/exe/zip/pdf 拒绝 |
| Magic Number | 扩展名是 png、内容是 HTML 时拒绝 |
| 文件大小 | 超过 10MB 拒绝；Spring 超额上传统一返回「文件大小不能超过10MB」 |
| 路径穿越 | `../../escape.png` 仍落到 uploads 下的 UUID 文件 |
| 伪造 Content-Type | 类型与扩展名/内容不一致拒绝 |
| 权限 | 无 `FILE_UPLOAD` 返回 403；有权限返回 200 |
| 并发 | 10 路同时上传，objectKey 不重复 |

## 8. Docker

`docker-compose.yml` 的 `app` 服务增加：

```yaml
volumes:
  - ./data/uploads:/app/data/uploads
```

本机未执行完整的 `docker compose down/up` 持久化回归，因为当前联调使用的是已有 MySQL 容器 + 本机 Backend。卷挂载已写好，容器重启后理论上会保留 `./data/uploads`。

## 9. 真实集成测试

环境：MySQL `edu-learning-mysql:3307`、Redis、Backend `8088`。浏览器点击未执行。

已完成：

1. 管理员登录，权限含 `FILE_UPLOAD`（共 9 个）
2. 上传 PNG，返回 `/uploads/...`，磁盘存在 `data/uploads/...`
3. `GET /uploads/...` 返回 200
4. 创建文章：封面 URL + 正文插入相对路径图片，发布后公开详情仍保留 `src="/uploads/..."`
5. 创建视频：封面和二维码使用上传 URL，发布后公开详情可读
6. 重新打开管理端详情，封面/二维码 URL 仍在
7. 上传 txt 被拒绝
8. 测试内容已下线删除，关键词 `LIVE04F` / `LIVE04F2` 残留为 0
9. 8088 进程在联调结束后已停止

小程序代码未修改。公开字段 `coverUrl` / `body` / `qrCodeUrl` 仍可按原接口读取；相对路径图片需要小程序侧拼上 API 域名才能显示，这是本地存储阶段的已知限制。

## 10. 风险

- Local Storage 依赖本机或容器磁盘；多实例部署时不能共享文件，除非改对象存储
- 未来切 COS 时只需实现 `StorageService` 并改 `storage.type`，业务字段继续存 URL
- 上传后未保存文章会留下孤儿文件；本 Sprint 不做垃圾回收
- 没有 CDN；本地静态资源只加了 `Cache-Control: max-age=86400`
- 相对 URL 在管理端通过 API base 拼接预览；小程序若直接用相对路径，需要后续统一成绝对地址或 CDN 域名
- HTML 清洗不能替代完整安全扫描；仍禁止 SVG
