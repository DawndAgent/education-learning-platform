# 生产部署：腾讯云 COS + 单机 Docker Compose

本指南把 API、MySQL、Redis、管理后台部署到一台 Linux 云主机，上传文件写入腾讯云 COS。

## 1. 腾讯云准备

### 1.1 COS 存储桶

1. 控制台创建存储桶，地域与 CVM 尽量相同（如 `ap-guangzhou`）。
2. 访问权限建议：**公有读私有写**（便于小程序直接播放 MP4 / 展示封面）。更严场景可改私有桶 + CDN 鉴权，需另行配置。
3. 记录：
   - `COS_REGION`（如 `ap-guangzhou`）
   - `COS_BUCKET`（如 `edu-media-1250000000`）
   - 默认访问域名：`https://{bucket}.cos.{region}.myqcloud.com`  
     填入 `COS_PUBLIC_BASE_URL`（**不要**末尾斜杠）。若已绑 CDN，填 CDN 的 `https://` 域名。
4. 桶 CORS（浏览器直链预览一般不强制；若以后做浏览器直传再补）：允许你的管理后台来源 `GET/HEAD/PUT`。

### 1.2 密钥

使用子账号密钥，仅授予该桶读写（`PutObject` / `GetObject` / `DeleteObject` / `HeadObject`）。  
填入 `COS_SECRET_ID`、`COS_SECRET_KEY`。**不要**把密钥提交到 Git。

### 1.3 云主机与域名

- CVM 建议 2 核 4G 起，已安装 Docker 与 Docker Compose 插件。
- 域名 A 记录指向该机公网 IP。
- HTTPS：上线前用 Certbot / 腾讯云证书挂到 Nginx（当前示例监听 80，证书挂载方式见文末）。

### 1.4 微信小程序（部署通后再配）

- `request` 合法域名：API 域名（如 `api.example.com`）
- `downloadFile` 合法域名：COS 或 CDN 域名（封面、本地视频、资料、文章内小程序码图）
- 文章正文「插入视频二维码」会调用微信 `getwxacodeunlimit`，需在 `deploy/.env` 配置：
  - `WECHAT_MINIAPP_APP_ID`（与 `project.config.json` 的 appid 一致）
  - `WECHAT_MINIAPP_APP_SECRET`（公众平台 → 开发设置）
  - `WECHAT_MINIAPP_ENV_VERSION=release`（体验版联调可改 `trial` / `develop`）
  - `WECHAT_MINIAPP_MOCK=false`（生产禁止 mock；mock 码无法被微信扫开小程序）

## 2. 构建管理后台静态包

在开发机或 CI 上：

```bash
cd admin-web
# 编辑 .env.production，将 VITE_API_BASE_URL 改为 https://你的API域名（无尾斜杠）
# 若 Nginx 与 API 同域反代，可写成 https://你的域名
npm ci
npm run build
rm -rf ../deploy/admin-dist/*
cp -R dist/* ../deploy/admin-dist/
```

## 3. 配置环境变量

```bash
cp deploy/.env.example deploy/.env
# 编辑 deploy/.env，填入 DB / JWT / ADMIN_ORIGIN / COS_* / PUBLIC_API_ORIGIN
```

注意：

- `JWT_SECRET` 至少 32 字节随机串。
- `ADMIN_ORIGIN` 为浏览器访问管理后台的来源，用于 CORS（如 `https://admin.example.com`；若同域托管可与 API 同域名）。
- 生产默认 `storage.type=cos`（见 `application-prod.yml`）。

## 4. 启动

在仓库根目录：

```bash
docker compose --env-file deploy/.env -f docker-compose.prod.yml up -d --build
```

健康检查：

```bash
curl -sS http://127.0.0.1/api/health
# 期望 JSON 成功响应
```

查看日志：

```bash
docker compose --env-file deploy/.env -f docker-compose.prod.yml logs -f app
```

## 5. 验收上传

1. 浏览器打开管理后台（Nginx 根路径）。
2. 登录后上传封面或本地 MP4。
3. 接口返回的 `url` 应为 `https://{COS_PUBLIC_BASE_URL}/images/...` 或 `/videos/local/...`。
4. 公网可直接打开该 URL；小程序 `production` 的 `apiBaseUrl` 指向同一 API 域名后即可展示/播放。

小程序修改 [`miniprogram/config/env.ts`](../miniprogram/config/env.ts)：

```ts
const currentEnv: AppEnv = 'production'
// production: 'https://你的API域名'
```

重新编译并上传体验版。

## 6. 本地开发不受影响

本机继续：

```bash
# storage.type=local（application.yml 默认）
./mvnw spring-boot:run
```

无需 COS 密钥。

## 7. 存量本地文件（可选）

若 `data/uploads` 里已有文件，可用 [COSCLI](https://cloud.tencent.com/document/product/436/63143) 同步到桶内相同 objectKey 前缀（`images/`、`videos/`、`files/`）。数据库里若仍是 `/uploads/...` 相对路径，需批量改成 COS 绝对 URL，或暂时保留 Nginx `/uploads` 反代到本机卷（当前生产 compose 未挂载本地 uploads，需自行加 volume 才兼容）。

## 8. HTTPS 提示

示例 `deploy/nginx.conf` 仅监听 80。上线前建议：

- 使用 Certbot 生成证书，增加 `listen 443 ssl`；或
- 在腾讯云 CLB / CDN 终结 TLS，回源 80。

## 9. 默认管理员

首次 Flyway 若仍包含开发种子账号，**部署后立即修改密码**。生产环境务必使用独立 `JWT_SECRET`。
