# Sprint 04-E 完成报告

文章和视频继续使用 Sprint 02 的 Article、Video、Content 模块。没有第二套 Controller、Service 或表。小程序目录没有改动。公开接口字段保持向后兼容。

本 Sprint 把内容编辑从主表补到正文和视频配置：文章可以写正文、预览、存草稿、发布和下线；视频可以配置来源、地址、封面、二维码和时长，再按同样的状态流转。二维码只保存图片 URL，不上传文件，也不调用微信或腾讯接口。

## 1. 完成内容

### Article

- 新增和编辑走已有 `POST /admin/api/articles`、`PUT /admin/api/articles/{contentId}`、`GET /admin/api/articles/{contentId}`。
- 保存时同时写 Content 和 Article，状态保持 `DRAFT`，不会自动发布。
- 正文使用 wangEditor。工具栏保留标题、正文、粗体、斜体、下划线、颜色、字号、对齐、列表、链接、撤销、重做，以及按 URL 插入图片。上传图片、上传视频和视频菜单已排除。
- 保存前用 Jsoup Safelist 清洗 HTML，去掉 `script`、`iframe`、`object`、`embed`、`javascript:` 和 `onerror` / `onclick` / `onload`。
- 预览用对话框展示标题、作者、来源和正文，不写数据库。
- 发布调用 `POST /admin/api/contents/{id}/publish`。前端和后端都会检查标题、二级分类和正文。正文为空返回「文章正文不能为空」。
- 下线调用 `POST /admin/api/contents/{id}/offline`。下线后编辑页仍可编辑。

### Video

- 新增和编辑走已有 `POST /admin/api/videos`、`PUT /admin/api/videos/{contentId}`、`GET /admin/api/videos/{contentId}`。
- 来源只允许 `WECHAT_CHANNEL`、`TENCENT_VIDEO`，页面显示为「微信视频号」「腾讯视频」。`BILIBILI` 等非法值在反序列化时返回 400。
- 草稿必须有标题、二级分类和来源。视频地址、二维码地址、封面可以先空着。`video_url` 列是 `NOT NULL`，空草稿存空字符串，没有改表。
- 发布必须有合法的 `http`/`https` 视频地址和二维码地址。缺少时返回「视频地址不能为空」或「二维码地址不能为空」，格式不对时返回「视频地址不合法」或「二维码地址不合法」。
- 时长仍按秒存在 `INT`。页面同时显示秒数和 `mm:ss`，例如 65 显示 `01:05`，3665 显示 `61:05`。
- 预览只展示标题、来源、地址、封面、二维码、时长和简介，没有播放器。二维码用 `el-image` 放大；图片加载失败时显示「二维码加载失败」。

### Preview / QR Code / Publish / Offline

- 文章和视频编辑页都有「返回内容列表」「保存草稿」「预览」「发布」。已发布后显示「下线」。
- 发布确认文案包含「发布后该内容将在小程序端可见。」下线确认文案是「确认下线」。
- 离开已修改页面时确认「当前内容尚未保存，确定离开吗？」，并注册了 `beforeunload`。
- 内容列表用「新增文章」「新增视频」进入对应编辑页。文章的「编辑」进入 `/articles/{id}`，视频的「编辑」进入 `/videos/{id}`。
- `ARTICLE` 和 `VIDEO` 创建后不能互换。已发布内容仍须先下线再逻辑删除，并同时逻辑删除文章或视频子表。

## 2. 修改文件

### 新增

- `src/main/java/com/xxedu/learning/common/util/HtmlSanitizer.java`：文章 HTML 清洗。
- `src/main/java/com/xxedu/learning/common/util/HttpUrls.java`：视频地址和二维码地址校验。
- `admin-web/src/composables/useUnsavedLeave.ts`：未保存离开确认。
- `docs/sprint-04-e-report.md`：本报告。

### 修改

- `pom.xml`：增加 Jsoup 1.18.3。Surefire 增加 `-XX:+EnableDynamicAgentLoading`，否则当前 JDK 21.0.12 无法挂上 Mockito，整套测试起不来。
- `ArticleService.java`：保存前清洗正文；正文清洗后没有文字则拒绝。
- `VideoService.java`：草稿允许空地址；非空地址必须是 http(s)。
- `ContentService.java`：发布文章检查正文；发布视频检查来源、视频地址和二维码地址。
- `VideoCreateRequest.java`、`VideoUpdateRequest.java`：视频地址改为草稿可空，发布时再由服务校验。
- `ArticleServiceTest.java`、`VideoServiceTest.java`、`ContentServiceTest.java`：补 XSS、空正文、停用分类、非法 URL、发布缺字段和非法来源。
- `ArticleTransactionTest.java`、`VideoTransactionTest.java`：补更新失败时主表回滚。
- `admin-web/src/api/article.ts`、`admin-web/src/api/video.ts`：补创建接口封装。
- `admin-web/src/utils/article-form.ts`、`video-form.ts`、`content-form.ts`：草稿/发布校验、时长格式、预览 HTML、编辑入口。
- `admin-web/src/components/RichTextEditor.vue`：保留网络图片，去掉上传。
- `admin-web/src/views/articles/editor.vue`、`videos/editor.vue`：编辑、预览、发布、下线。
- `admin-web/src/views/contents/index.vue`：新增文章/视频，按类型进入编辑页。
- `admin-web/src/router/index.ts`：增加 `/articles/create`、`/videos/create`，没有重做整个路由。
- `admin-web/tests/editor.test.mjs`、`content.test.mjs`：对齐新页面和校验。

### 删除

无。

## 3. Backend API

这些接口都是原有路径，本 Sprint 只收紧校验和清洗，没有新开一套。

| Method | Path | Request | Response | Permission |
| --- | --- | --- | --- | --- |
| POST | `/admin/api/articles` | title、categoryId、sort、body、author、source、coverUrl、summary | ArticleDetailVO | CONTENT_CREATE |
| PUT | `/admin/api/articles/{contentId}` | 同上 | ArticleDetailVO | CONTENT_UPDATE |
| GET | `/admin/api/articles/{contentId}` | — | ArticleDetailVO | CONTENT_VIEW |
| POST | `/admin/api/videos` | title、categoryId、sort、sourceType、videoUrl、qrCodeUrl、coverUrl、summary、duration | VideoDetailVO | VIDEO_MANAGE |
| PUT | `/admin/api/videos/{contentId}` | 同上 | VideoDetailVO | VIDEO_MANAGE |
| GET | `/admin/api/videos/{contentId}` | — | VideoDetailVO | CONTENT_VIEW |
| POST | `/admin/api/contents/{id}/publish` | — | ContentDetailVO | CONTENT_PUBLISH |
| POST | `/admin/api/contents/{id}/offline` | — | ContentDetailVO | CONTENT_OFFLINE |

公开接口没有改路径和字段语义：

- `GET /api/articles/{contentId}` 仍返回 `body`、`author`、`source`。未发布或不存在时仍是「文章不存在」。
- `GET /api/videos/{contentId}` 仍返回 `sourceType`、`videoUrl`、`qrCodeUrl`、`duration`。

视频写入权限仍是 Sprint 02 的 `VIDEO_MANAGE`，不是 `CONTENT_UPDATE`。发布和下线仍用内容权限。前端隐藏按钮，后端继续校验。

## 4. 数据库

No database migration required.

没有新增 Flyway 版本，也没有改已有表。Article 仍是 `body`、`author`、`source`。Video 仍是 `source_type`、`video_url`、`cover_url`、`qr_code_url`、`duration`。

`content.cover_url` 和 `video.cover_url` 都保留。编辑页只填一个封面，保存时写到内容主表，并同步到视频行。视频行上的 `title`、`cover_url`、`status` 仍是内容主表的副本，分类和状态以 Content 为准。本 Sprint 不做表结构重构。

## 5. 测试结果

```text
mvn test
Tests run: 73, Failures: 0, Errors: 0, Skipped: 0

mvn verify
PASS（同一套测试，加上 Checkstyle 和 SpotBugs，退出码 0）

npm run build
PASS

npx tsc --noEmit
PASS

npx tsc -p tsconfig.app.json --noEmit
PASS
```

Admin Web 的 node:test 为 18 项，全部通过。

## 6. 真实集成测试

MySQL `edu-learning-mysql`（3307）和本机 Redis 已连通。后端用 dev 配置跑在 8088。浏览器点击未执行，没有浏览器自动化工具。

已用管理员登录完成：

- 创建文章，正文含 `<script>` 和 `onerror`。保存后正文只剩安全段落和图片，状态为 `DRAFT`。公开详情返回「文章不存在」。
- 发布后状态为 `PUBLISHED`。`GET /api/articles/{contentId}` 返回清洗后的 `body`，以及 `author`、`source`。
- 下线后状态为 `OFFLINE`，公开详情再次返回「文章不存在」。然后逻辑删除。
- 创建视频，来源 `WECHAT_CHANNEL`，地址先留空。发布返回「视频地址不能为空」。
- 补上腾讯视频地址、二维码和时长 3665 后发布。公开详情返回 `TENCENT_VIDEO`、`videoUrl`、`qrCodeUrl`、`duration`。
- 下线后状态为 `OFFLINE`，再删除。`sourceType=BILIBILI` 返回 400「请求体无法解析」。
- 关键词 `LIVE04E` 查询结果为 0。测试数据已清掉。8088 进程在联调结束后已停止。

## 7. 未完成事项

按本 Sprint 边界，这些没有做，也不应在本 Sprint 继续做：

- 文件上传、对象存储、COS/OSS SDK
- 视频上传、转码、播放器
- 微信视频号、腾讯视频、微信开放平台 API
- 自动生成真实小程序码
- 支付、会员、评论、收藏、学习记录、AI
- 小程序端修改
- 浏览器里逐页点击。接口联调已完成，页面点击未做。

## 8. 风险

- 富文本清洗依赖 Jsoup Safelist。样式里的冷门 XSS 仍可能漏掉，预览又在管理端用 `v-html` 展示。小程序端原有的标签过滤没有改，两层都在，但不能当成完整的 HTML 安全方案。
- 管理端 token 仍在 `localStorage` 的 `admin_token`。退出登录不拉黑 JWT。
- 二维码和视频地址都是外部 URL。地址失效或图片打不开时，页面只显示加载失败，不会替用户检查第三方是否可访问。
- 草稿的空视频地址存的是空字符串，因为 `video_url` 不能为 NULL。发布时会拒绝这个空值。
- wangEditor 体积使构建出现超过 500 kB 的 chunk 警告。这是编辑器本身的体积，不是功能失败。
- 本机 JDK 21.0.12 需要 `-XX:+EnableDynamicAgentLoading` 才能跑 Mockito。这项只加在测试 JVM 参数上。
