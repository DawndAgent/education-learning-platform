# Sprint 06 完成报告

## 1. Sprint Summary

在 Sprint 04-E/F/I 已具备的内容生产能力之上，收口运营闭环：创建入口权限对齐、禁止 Content 壳创建、编辑页保存/发布文案与「重新发布」、生产路由权限守卫，并用 `ContentProductionLoopTest` 覆盖 **创建 → 草稿 → 发布 → 公开可见 → 浏览量 → 编辑不改状态 → 下线 → 再发布**。

未改动 Sprint 05 Mini Program 核心结构。

**Status: PASS**

---

## 2. Content Production Flow

```text
Dashboard / Content List
  → 新建文章（CONTENT_CREATE）/ 新建视频（VIDEO_MANAGE）
  → Article / Video 编辑页（一次请求事务创建 Content + 子表）
  → 保存草稿 / 本地预览 / 保存后发布
  → Public API 可见
  → 下线 → Public 不可见
  → 重新发布 → Public 恢复
```

壳创建路径 `POST /admin/api/contents` 已对管理端 HTTP **拒绝**（避免无 Article/Video 子表脏数据）。`ContentService.create` 仍保留给历史单测与兼容场景。

---

## 3. Article Flow

| 步骤 | 实现 |
| ---- | ---- |
| 新建 | `POST /admin/api/articles`（事务）→ 跳转 `/articles/{id}` |
| 编辑字段 | 标题、分类、封面、摘要、作者、来源、正文 |
| 保存 | 允许未达发布条件的草稿；已发布/下线编辑保存不改状态 |
| 预览 | 当前表单数据（可未保存） |
| 发布 | 校验 → 先保存 → `POST .../publish` |
| 下线 / 再发布 | 既有 API；OFFLINE 按钮文案「重新发布」 |

---

## 4. Video Flow

| 步骤 | 实现 |
| ---- | ---- |
| 新建 | `POST /admin/api/videos`（需 `VIDEO_MANAGE`） |
| 字段 | 标题、分类、摘要、封面、来源、视频 URL、二维码、时长 |
| 来源中文 | 微信视频号 / 腾讯视频 |
| 发布规则 | 沿用 Sprint 04-E（地址 + 二维码必填且合法 URL） |
| 预览 / 发布 / 下线 | 与 Article 同模式 |

---

## 5. Publish / Offline Flow

- 发布幂等：已是 `PUBLISHED` 时不重置 `publishTime`
- `DRAFT/OFFLINE → PUBLISHED`：写入 / 刷新 `publishTime`
- 已发布内容仅编辑保存：状态仍为 `PUBLISHED`，`publishTime` 不变
- 下线后 Public API 404；再发布后恢复
- `viewCount`：仅 PUBLISHED 可 `POST /api/content/{id}/view`

---

## 6. Permission

| 能力 | 前端 | 后端 |
| ---- | ---- | ---- |
| 新建文章 | `CONTENT_CREATE`（Dashboard / 列表 / 路由） | `CONTENT_CREATE` |
| 新建视频 | `VIDEO_MANAGE`（已与列表对齐） | `VIDEO_MANAGE` |
| 编辑文章 | `CONTENT_UPDATE` | 同左 |
| 编辑视频 | `VIDEO_MANAGE` | 同左 |
| 发布 / 下线 | `CONTENT_PUBLISH` / `CONTENT_OFFLINE` | 同左 |
| 列表编辑按钮 | `visibleActions.edit` 改为 `canOpenEditor` | — |

路由守卫：`/articles/create`、`/videos/create`、对应编辑页无权限时回跳 Dashboard。

---

## 7. Transaction Consistency

- `ArticleService.create` / `VideoService.create`：`@Transactional`（既有）
- `ArticleTransactionTest` / `VideoTransactionTest`：子表失败回滚主表（既有，本 Sprint 复跑 PASS）
- 管理端不再走「先 Content 后子表」的两步 HTTP 创建

---

## 8. API Changes

```text
POST /admin/api/contents
  → 400「请使用文章或视频接口创建内容」（行为变更）

其余路径无破坏性变更：
POST /admin/api/articles
PUT  /admin/api/articles/{contentId}
POST /admin/api/videos
PUT  /admin/api/videos/{contentId}
POST /admin/api/contents/{id}/publish|offline|duplicate
```

---

## 9. Database Changes

```text
No migration.
```

---

## 10. Test Results

| Command | Result |
| ------- | ------ |
| `mvn test`（含于 verify） | PASS |
| `mvn verify` | PASS |
| `ContentProductionLoopTest` | PASS |
| `admin-web` `npm run build` | PASS |
| Admin unit tests（dashboard/content/editor） | PASS |
| Mini Program `npm run typecheck` / `npm run check` | PASS（无结构性改动） |
| `npx tsc --noEmit`（admin vue-tsc via build） | PASS |

---

## 11. Real Integration

| 项 | 结果 |
| -- | ---- |
| MockMvc 生产闭环（Article + Video + Public + view） | PASS |
| 本地 Backend `http://localhost:8088` | NOT RUN（健康检查超时，进程未起） |
| Admin Web 浏览器联调（上传封面/正文图） | NOT RUN（依赖本地 Backend） |

---

## 12. Mini Program Status

```text
Code-level: PASS（Sprint 05 保持；本 Sprint 无大型改动）
IDE/Real-device: NOT RUN
```

---

## 13. Bugs Fixed

- Dashboard「新建视频」误用 `CONTENT_CREATE`，与列表/后端 `VIDEO_MANAGE` 不一致
- 管理端可经 `POST /admin/api/contents` 造无子表壳内容
- 列表 `visibleActions.edit` 对视频未按 `VIDEO_MANAGE` 判断
- 编辑已发布内容保存仍提示「已保存草稿」

---

## 14. Known Risks

- 本地未起 Backend 时，未做真实上传联调（封面/二维码/正文图）
- `ContentService.create` 仍可被服务层调用造壳（仅 HTTP 管理入口关闭）
- 微信真机 / 开发者工具仍未验收（承接 Sprint 05）

---

## 15. Technical Debt

- `contentApi.createContent` / `toCreatePayload` 前端仍保留但 UI 不用，可后续删除
- 路由权限无独立 403 页，无权限统一回 Dashboard
- 孤儿上传文件清理仍未做（Sprint 04-F 已知）

---

## 16. Recommended Next Sprint

- 启动本地 Backend + Admin，完成上传与发布端到端人工验收
- 微信开发者工具补齐 Sprint 05 真机链路
- 可选：彻底移除 `ContentService.create` 的 ARTICLE/VIDEO 壳能力，或改为自动建空子表

---

Sprint 06 completed.
Report: docs/sprint-06-report.md
Status: PASS
