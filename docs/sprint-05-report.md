# Sprint 05 完成报告

## 1. Sprint Summary

将 Mini Program 从技术可用版本升级为 V1 内容产品入口：首页产品化（品牌 / 搜索入口 / 栏目 / 最新内容）、搜索分页、详情分享与深链、浏览量统计、空/错/加载态统一，并与 Backend 公共 API 对齐。未引入登录、会员、站内播放、推荐算法或 Elasticsearch。

**Status: PARTIAL**（自动化与构建通过；微信开发者工具 E2E 未跑）

---

## 2. Product Changes

| 能力 | 结果 |
| ---- | ---- |
| 首页品牌 + 搜索入口 | 完成 |
| 三大栏目（分类树 API） | 复用并保留 |
| 最新内容（≤10，publishTime DESC） | 完成 |
| 分类页子栏目 + 最新内容 | 完成 |
| 内容列表分页 / 刷新 / 上拉 | 复用 Sprint 03 |
| 搜索（标题 keyword + 分页 + 本地历史） | 完成 |
| Article / Video 详情产品化 | 完成 |
| 分享 + 深链打开详情 | 完成 |
| 浏览量 viewCount | 完成 |
| 热门内容（viewCount 排序） | 未做（优先最新内容，避免首页拥挤） |
| Banner CMS | 未做（无数据模型，按规范跳过） |

---

## 3. Mini Program Changes

- 新增 `pages/search/`：搜索框、结果卡片、分页、下拉刷新、上拉加载、本地历史（最多 10 条）
- 首页：品牌区、搜索入口、栏目导航、最新内容（`sort=publishTime`，pageSize=10）
- 分类页：子栏目 + 该分类最新内容
- `content-card`：封面失败占位；统一展示封面 / 标题 / 摘要 / 类型 / 时间
- 详情：摘要展示；视频文案「微信视频号观看 / 腾讯视频观看 / 扫码观看完整视频」；`onShareAppMessage`；详情成功后静默 `POST /view`
- Service：`recordContentView`；类型增加可选 `sort`
- 生命周期：列表 / 首页 / 搜索仍不在 `onShow` 无脑重拉；返回依赖页面栈保位

---

## 4. Backend Changes

- `ContentQueryRequest.sort`：`publishTime` 时公共列表按 `publish_time DESC`
- `ContentPageQuery.orderByPublishTime` + Mapper `<choose>` 排序
- `ContentService.recordView`：仅 `PUBLISHED` 且 `deleted=0` 原子 `view_count + 1`，否则 404
- `ContentPublicController`：`POST /api/content/{id}/view`
- `SecurityConfig`：放行 `POST /api/content/*/view`
- 测试：`ContentPublicOpsTest`（keyword / empty keyword / view 规则）

---

## 5. API Changes

```text
GET /api/content?keyword=&sort=publishTime&pageNum=&pageSize=
  - 仍仅返回 PUBLISHED
  - keyword：title LIKE（已有能力，本 Sprint 验证）
  - sort=publishTime：发布时间倒序（新增可选参数，默认行为不变）

GET /api/content/{id}          （不变，仅 PUBLISHED）
GET /api/articles/{contentId}  （不变）
GET /api/videos/{contentId}    （不变）

POST /api/content/{id}/view
  - 匿名可访问
  - 成功：viewCount + 1
  - DRAFT / OFFLINE / 不存在：404
```

---

## 6. Database Changes

```text
No migration
```

复用既有 `content.view_count` 字段。

---

## 7. Search

- 后端：`title LIKE CONCAT('%', escapedKeyword, '%') ESCAPE '!'`（既有 `likePattern`）
- 前端：`GET /api/content?keyword=...&sort=publishTime&pageNum&pageSize=10`
- 空关键字：提示「请输入搜索关键词」，不发无效业务搜索
- 无结果：「暂无相关内容」
- 失败：「搜索失败，请稍后重试」
- 本地历史：`wx.setStorageSync`，最多 10 条

---

## 8. View Count

- API：`POST /api/content/{id}/view`
- 时机：详情 `status === success` 之后调用一次
- 原子 SQL：

```sql
UPDATE content
SET view_count = view_count + 1
WHERE id = ?
  AND status = 'PUBLISHED'
  AND deleted = 0
```

- 前端不可写 viewCount；失败静默
- 测试：`ContentPublicOpsTest.recordViewIncrementsPublishedOnly`

---

## 9. Share

- Article / Video 共用详情页 `onShareAppMessage`
- title：内容标题（缺省品牌名）
- path：`/pages/content-detail/content-detail?id={id}`
- imageUrl：优先 `coverUrl`
- 深链：`onLoad` 只读 `id` 拉详情，不依赖首页参数

---

## 10. Test Results

| Command | Result |
| ------- | ------ |
| `mvn test`（含于 verify） | PASS |
| `mvn verify` | PASS |
| `npm run typecheck`（`tsc --noEmit`） | PASS |
| `npm run check` | PASS |
| `npm run test:mp` | PASS（47 tests） |
| `npm run build` | N/A（小程序无独立 build 脚本，以 typecheck + check 为准） |

---

## 11. WeChat Developer Tools

```text
Mini Program IDE E2E：NOT RUN
Reason: current environment does not provide WeChat Developer Tools
```

---

## 12. Business Flow Verification

| Flow | Result | Note |
| ---- | ------ | ---- |
| Home | PASS | 单元 / 结构检查 |
| Category | PASS | 单元 / 结构检查 |
| Content List | PASS | Sprint 03 复用测试 |
| Article | PASS | Sprint 03d + 文案更新 |
| Video | PASS | Sprint 03d + 文案更新 |
| Search | PASS | sprint-05 + Backend keyword |
| Share | PASS | 代码路径 + 断言存在 onShareAppMessage |
| Refresh | PASS | 列表 / 搜索复用 session |
| Load More | PASS | 并发保护测试 |
| Error State | PASS | |
| Empty State | PASS | |
| WeChat IDE 真机链路 | NOT RUN | 见第 11 节 |

---

## 13. Bugs Fixed

- 公共 `POST /api/content/{id}/view` 曾被 Security 默认 deny（仅 GET 放行）→ 已单独 permitAll
- 详情下线/缺失文案统一为「内容不存在或已下线」
- 首页最新内容 pageSize 从 6 调整为规范建议的 10，并按 publishTime 排序

---

## 14. Known Risks

- 微信开发者工具未做端到端验证（分享卡片、长按识码、真机返回栈）
- viewCount 无防刷，同一用户可多次 +1（V1 接受）
- 富文本图片点击预览未做（rich-text 取图成本高，按规范暂缓）
- 分类树未做本地 TTL 缓存（可后续轻量优化）

---

## 15. Technical Debt

- 搜索历史仅本地，无跨端同步
- 首页未做热门内容区块（刻意省略）
- 详情分享默认图未配置静态资源（无 cover 时仅 title/path）

---

## 16. Recommended Next Sprint

- 微信开发者工具完整验收与真机分享回归
- 可选：首页热门（`viewCount DESC`，仍无推荐算法）
- 可选：分类树短时缓存、文章图片 previewImage
- 仍不做：登录、会员、收藏、站内播放、推荐系统

---

Sprint 05 completed.
Report: docs/sprint-05-report.md
Status: PARTIAL
