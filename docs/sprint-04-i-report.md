# Sprint 04-I 完成报告

## 1. Sprint Summary

在不扩大 V1 业务边界的前提下，增强了内容运营效率：批量下线 / 批量删除、内容复制、列表预览、删除后分页回退、操作反馈与权限按钮。搜索、草稿脏检查、编辑页预览等已有能力复用保留。

**Status: PASS**

---

## 2. Feature List

| Feature | Result |
| ------- | ------ |
| Content Search | PASS（复用既有 keyword/category/type/status，page 重置） |
| Batch Offline | PASS |
| Batch Delete | PASS |
| Duplicate | PASS |
| Preview | PASS（列表 Dialog + 编辑页既有预览） |
| Draft UX | PASS（复用 saving / dirty / useUnsavedLeave） |
| Permission | PASS |
| Operation Feedback | PASS |
| List refresh / page after delete | PASS |

---

## 3. Backend Changes

- `ContentService.duplicate`：事务复制 Content + Article/Video；状态 `DRAFT`；不复制 `viewCount` / `favoriteCount` / `publishTime`
- `ContentService.batchOffline`：全部成功或全部失败；仅允许 `PUBLISHED`
- `ContentService.batchDelete`：全部成功或全部失败；拒绝 `PUBLISHED`
- `AuthContext.hasPermission`：视频复制额外校验 `VIDEO_MANAGE`
- DTO/VO：`ContentBatchRequest`、`ContentBatchResultVO`
- Controller：`POST .../duplicate`、`POST .../batch-offline`、`POST .../batch-delete`
- 测试：`ContentOpsApiTest`

---

## 4. Admin Web Changes

- 内容列表：多选、已选计数、批量下线/删除、复制、预览、创建时间列
- `ContentPreviewDialog`：按类型拉取 Article/Video 真实详情
- `contentApi`：duplicate / batchOffline / batchDelete
- `content-form`：复制权限、批量确认文案、删除后页码计算
- 成功文案统一为「发布成功 / 下线成功 / 删除成功」

---

## 5. API Changes

```text
POST /admin/api/contents/{id}/duplicate
Permission: CONTENT_CREATE（VIDEO 另需 VIDEO_MANAGE）

POST /admin/api/contents/batch-offline
Body: { "ids": ["1","2"] }
Permission: CONTENT_OFFLINE
Response data: { "successCount": 2, "failedCount": 0 }

POST /admin/api/contents/batch-delete
Body: { "ids": ["1","2"] }
Permission: CONTENT_DELETE
Response data: { "successCount": 2, "failedCount": 0 }
```

ID 继续按项目约定：JSON 字符串。

---

## 6. Database Changes

```text
No database migration.
```

---

## 7. Test Results

| Command | Result |
| ------- | ------ |
| `mvn test` / `mvn verify` | PASS |
| `npm run build` | PASS |
| `npx tsc --noEmit` | PASS |
| `node --test tests/content.test.mjs` | PASS |
| `npm run lint` | NOT RUN（无 lint script） |

---

## 8. Integration Test

MySQL(`3307`) + Redis + Backend(`8088`) 实测：

```text
Login → create/publish articles
→ duplicate (DRAFT, viewCount=0)
→ batch-offline successCount=2
→ batch-delete successCount=3
→ video duplicate DRAFT
→ cleanup + logout
→ public /api/categories/tree + /api/content 仍 code=0
```

`LIVE_04I_PASS`

---

## 9. Browser Test

```text
Browser E2E：未执行，原因：当前环境没有浏览器自动化能力
```

---

## 10. Bugs Fixed

本 Sprint 以增强为主。顺带修正：

- 删除最后一页最后一条后，页码回退（`nextPageAfterDelete`），避免空页

---

## 11. Known Risks

- 批量采用「全部成功 / 全部失败」事务，混合状态会整批拒绝（符合设计）
- 列表预览依赖 `CONTENT_VIEW` + Article/Video 详情接口；无正文的空壳 ARTICLE 预览可能几乎空白
- 批量按钮在选中含不可操作状态时禁用，需运营理解「只能下线已发布 / 只能删除草稿与已下架」

---

## 12. Technical Debt

- 无前端 E2E
- 无 ESLint
- 批量未做部分成功明细（当前刻意全有或全无）
- 复制标题未自动加「（副本）」后缀（按规格原样复制）

---

## 13. Recommended Next Sprint

1. 运营账号 / 角色细分（若需要非 ADMIN）
2. 操作审计日志
3. 浏览器 E2E
4. 对象存储（COS）替换本地上传

**不要自动开始 04-J。**
