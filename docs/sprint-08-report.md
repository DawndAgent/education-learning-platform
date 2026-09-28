# Sprint 08 完成报告

## 1. Sprint 目标

正式开放 Content 类型 **QUESTION / WEEKLY / DOCUMENT**，打通运营侧创建 → 编辑 → 预览 → 发布 → 小程序展示闭环；Topic 聚合同步支持这三类已发布内容。不做在线考试、判题、全文搜索、AI 解题等。

**Status: PASS**（自动化与构建通过；真实 Admin 浏览器 / 微信开发者工具 E2E NOT RUN）

---

## 2. 数据库变化

```text
V7__create_question_weekly_document_tables.sql
```

| 表 | 说明 |
| -- | ---- |
| `question` | 题目详情，`content_id` 唯一；题型 / 难度索引 |
| `weekly_question` | 每周一题，`week_label` + 题目/答案/解析 |
| `document` | 资料文件元数据（URL、文件名、大小、类型、预览/下载） |

索引（按规范，不超额）：

- QUESTION：`uk_question_content_id`、`idx_question_type`、`idx_question_difficulty`、`idx_question_deleted`
- WEEKLY：`uk_weekly_content_id`、`idx_weekly_week_label`、`idx_weekly_deleted`
- DOCUMENT：`uk_document_content_id`、`idx_document_file_type`、`idx_document_deleted`

历史 migration 未改动。

---

## 3. Content 类型变化

| 类型 | Sprint 08 |
| ---- | --------- |
| ARTICLE | 继续可写 |
| VIDEO | 继续可写 |
| QUESTION | **正式开放** |
| WEEKLY | **正式开放** |
| DOCUMENT | **正式开放** |
| TOPIC | **禁止作为 Content 创建**（Topic 独立模型，枚举保留兼容） |

`ContentService.assertWritableType` 白名单：ARTICLE / VIDEO / QUESTION / WEEKLY / DOCUMENT。创建后 `contentType` 仍不可变。

---

## 4. QUESTION

- 模块：`modules/question`（entity / mapper / dto / vo / service / admin+public controller）
- 题型 V1：`SINGLE_CHOICE` / `MULTIPLE_CHOICE` / `FILL_BLANK` / `ANSWER` / `PROOF` / `IMAGE_QUESTION`
- 难度：`EASY` / `MEDIUM` / `HARD`
- 内容：题目/答案/解析文字 + 可选图片；**非**在线答题系统
- 发布校验：title + 有效分类；题目文字或图片至少一个；答案文字或图片至少一个；解析可选

Admin：`POST/PUT/GET /admin/api/questions[/{contentId}]`  
Public：`GET /api/questions/{contentId}`（仅 PUBLISHED，否则 404）

---

## 5. WEEKLY

- 模块：`modules/weekly`，表 `weekly_question`（与 QUESTION **分表**）
- 核心字段：`weekLabel`（手动填写，如「2026年第39周」）+ 题目/答案/解析
- 发布校验：title + 分类 + weekLabel 非空；题目与答案至少各有文字或图片

Admin：`/admin/api/weeklies`  
Public：`/api/weeklies/{contentId}`

---

## 6. DOCUMENT

- 模块：`modules/document`
- V1：展示文件信息 + 预览/下载 URL（无复杂 PDF 阅读器）
- 上传：复用 `StorageService` + `UploadScene.DOCUMENT`；`DocumentUploadValidator`（Tika，不信任 Content-Type）
- 支持：PDF / DOC / DOCX / XLS / XLSX / PPT / PPTX（最大 20MB）
- 发布校验：title + 分类 + `fileUrl` + `fileName` 非空；缺省时 download/preview 回填为 fileUrl

Admin：`/admin/api/documents`  
Public：`/api/documents/{contentId}`

---

## 7. Admin Web

- 内容管理筛选与新建：题目 / 每周一题 / 资料（无 TOPIC 选项）
- 编辑器：`views/questions|weeklies|documents/editor.vue`
- 预览：`ContentPreviewDialog` 支持三类（答案/解析可折叠；资料展示文件信息）
- 路由：`/questions`、`/weeklies`、`/documents`（含 create / `:id`）
- 文件：`DocumentFileUpload` + `fileApi.uploadDocument`（scene=DOCUMENT）
- 专题内容选择器类型过滤扩展为五类可写 Content

权限复用：`CONTENT_*`；上传复用 `FILE_UPLOAD`。

---

## 8. Mini Program

- 列表/首页类型文案：题目 / 每周一题 / 资料（无「未知类型」）
- `content-detail` 扩展：question / weekly / document
- 题目与每周一题：答案、解析默认折叠（「查看答案」「查看解析」）
- 资料：文件信息；在线预览尽量 `downloadFile` + `openDocument`，失败则复制链接；下载资料 = 复制链接（不伪造下载能力）
- 专题详情点击路由到对应内容详情；仅展示 PUBLISHED 内容（Topic 发布规则未改状态机）

---

## 9. Topic 兼容

- `TopicService.addContents` 允许 ARTICLE / VIDEO / **QUESTION / WEEKLY / DOCUMENT**
- `TopicMapper.xml` 前台计数/列表 `content_type IN (...)` 同步扩展
- Topic 本身仍不是 Content；内容下线后专题前台自动隐藏该条

---

## 10. 权限

继续使用既有 Content 权限：

```text
CONTENT_VIEW / CREATE / UPDATE / DELETE / PUBLISH / OFFLINE
FILE_UPLOAD
```

未为 QUESTION / WEEKLY / DOCUMENT 单独建权限码。

---

## 11. API

| 区域 | 路径 |
| ---- | ---- |
| Admin | `/admin/api/questions`、`/weeklies`、`/documents` |
| Public | `/api/questions/{contentId}`、`/weeklies/{contentId}`、`/documents/{contentId}` |

统一 `ApiResponse` / 分页响应；Security 对 Public GET 放行。删除 Content 时同步逻辑删除子表行。

---

## 12. 测试结果

| Command | Result |
| ------- | ------ |
| `mvn test` | **PASS**（109 tests） |
| `mvn verify` | **PASS** |
| `QuestionServiceTest` / `WeeklyServiceTest` / `DocumentServiceTest` | **PASS**（CRUD、发布校验、Public Draft 404） |
| `ContentServiceTest` / `TopicServiceTest` / `FlywayMigrationTest` | **PASS** |
| `admin-web` `npm run build` | **PASS** |
| `admin-web` `tsc --noEmit` | **PASS** |
| Mini Program `npm run typecheck` | **PASS** |
| Mini Program `npm run test:mp` | **PASS**（48 tests） |
| WeChat IDE / 真机编译 | **NOT RUN** |
| 真实 MySQL + Admin 浏览器 Integration / E2E | **NOT RUN**（本环境未起完整联调栈） |

### 重点发布校验（单元覆盖）

| 场景 | Result |
| ---- | ------ |
| QUESTION 无题目 / 无答案 → 发布失败 | PASS |
| QUESTION 仅文字或仅图片可发布 | PASS |
| WEEKLY 无 weekLabel / 无题目 / 无答案 → 失败 | PASS |
| DOCUMENT 无文件 → 失败；有文件可发布 | PASS |
| Draft/Offline Public API → 404 | PASS |

---

## 13. 验收清单对照

```text
[x] QUESTION / WEEKLY / DOCUMENT 数据模型 + Flyway V7
[x] Content 类型正式开放（禁 TOPIC 作为 Content）
[x] 三类 Editor + Preview + Publish / Offline / Republish / Delete
[x] Public API + Mini Program Detail + List 类型展示
[x] Topic 支持三种新类型
[x] File Upload 复用 StorageService
[x] 权限正确；子表查询按列选取，列表不 N+1 拉详情
[x] Backend Test / Admin Build / tsc / MP typecheck
[ ] Integration Test（真实栈）— NOT RUN
[ ] E2E（浏览器 + 微信 IDE）— NOT RUN
[x] docs/sprint-08-report.md
```

---

## 14. 风险与债务

- 真实 Admin UI / 微信开发者工具人工 E2E 未跑
- DOCUMENT 小程序端下载仅为复制链接（受微信能力限制）
- V1 无数学公式编辑器、无 PDF 在线阅读器
- Surefire 增加 `-Djdk.attach.allowAttachSelf=true` 以兼容本机 JDK 21 Mockito attach

---

## 15. 明确未做（Sprint 08 禁止项）

在线考试、自动判题、答题记录、错题本、收藏/评论/点赞/积分、AI 解题/批改/出题、OCR、Elasticsearch 全文搜索、会员支付、学习进度档案等。
