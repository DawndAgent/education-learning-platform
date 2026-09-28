# Sprint 07 完成报告

## 1. 完成内容

实现独立「专题 / 内容聚合」体系（Topic 非 Content 类型）：

- Flyway `V6__create_topic_tables.sql`：`topic`、`topic_content` + 7 个 TOPIC_* 权限
- Backend `modules/topic`：Admin CRUD / 内容管理 / 排序 / 发布下线；Public 列表 / 精选 / 详情
- Admin Web：专题列表、编辑（添加内容、上下移排序、预览）、菜单与路由权限
- Mini Program：专题列表 / 详情；首页「专题精选」；详情内文章/视频复用现有 `content-detail`

**Status: PASS**（自动化与构建通过；真实 Admin/微信 IDE E2E NOT RUN）

---

## 2. 数据库变更

```text
V6__create_topic_tables.sql
```

| 表 | 说明 |
| -- | ---- |
| `topic` | 专题主表，状态 DRAFT/PUBLISHED/OFFLINE |
| `topic_content` | 多对多关联，逻辑唯一 `(topic_id, content_id, deleted)` |

权限种子：`TOPIC_VIEW/CREATE/UPDATE/DELETE/PUBLISH/OFFLINE/CONTENT_MANAGE` → 管理员角色。

---

## 3. Backend API

### Admin `/admin/api/topics`

| Method | Path | Permission |
| ------ | ---- | ---------- |
| GET | `/` | TOPIC_VIEW |
| GET | `/{id}` | TOPIC_VIEW |
| POST | `/` | TOPIC_CREATE |
| PUT | `/{id}` | TOPIC_UPDATE |
| DELETE | `/{id}` | TOPIC_DELETE（禁止删除 PUBLISHED） |
| POST | `/{id}/publish` | TOPIC_PUBLISH（至少 1 条已发布 ARTICLE/VIDEO） |
| POST | `/{id}/offline` | TOPIC_OFFLINE |
| GET | `/{topicId}/contents` | TOPIC_VIEW |
| POST | `/{topicId}/contents` | TOPIC_CONTENT_MANAGE |
| DELETE | `/{topicId}/contents/{contentId}` | TOPIC_CONTENT_MANAGE |
| PUT | `/{topicId}/contents/sort` | TOPIC_CONTENT_MANAGE |

### Public `/api/topics`

| Method | Path | 说明 |
| ------ | ---- | ---- |
| GET | `/` | 仅 PUBLISHED Topic |
| GET | `/featured` | 首页精选，最多 4 |
| GET | `/{id}` | 仅 PUBLISHED Topic + PUBLISHED Content |

发布规则：专题内 Draft/Offline Content 不出现在 Public；Content 下线后专题仍存在，前台自动隐藏该内容。

---

## 4. Admin Web

- 菜单：内容管理后增加「专题管理」
- `/topics` 列表：关键词 / 分类 / 状态、发布 / 下线 / 删除 / 预览
- `/topics/create`、`/topics/:id/edit`：基础信息 + ImageUpload + 内容添加 Dialog + ↑↓ 排序 + 保存排序
- 权限按钮与路由守卫按 TOPIC_* 控制

---

## 5. Mini Program

- `pages/topic-list`、`pages/topic-detail`
- `services/topic.ts`（禁止页面直调 `wx.request`）
- 首页：专题精选（`/api/topics/featured`）+ 「全部专题」入口
- 专题内容点击 → 现有 `content-detail`

---

## 6. 权限

```text
TOPIC_VIEW
TOPIC_CREATE
TOPIC_UPDATE
TOPIC_DELETE
TOPIC_PUBLISH
TOPIC_OFFLINE
TOPIC_CONTENT_MANAGE
```

管理员角色已绑定；`sys_permission` 总数 17。

---

## 7. 测试结果

| Command | Result |
| ------- | ------ |
| `mvn test` / `mvn verify` | PASS |
| `TopicServiceTest` / `TopicPublicApiTest` | PASS |
| `admin-web` `npm run build` | PASS |
| Mini Program `npm run typecheck` / `check` / `test:mp` | PASS（48 tests） |
| WeChat IDE / 真机 | NOT RUN |
| 真实 MySQL + Admin 浏览器 E2E | NOT RUN（本环境未起完整联调栈） |

---

## 8. E2E 验证

| 步骤 | Result |
| ---- | ------ |
| 创建专题 | PASS（单元/集成） |
| 添加内容 | PASS |
| 排序 | PASS |
| 预览 | PASS（Admin Dialog 代码） |
| 发布 | PASS |
| Mini Program 查看 | PASS（API + 页面结构；IDE NOT RUN） |
| 内容下线 → 专题隐藏该内容 | PASS（TopicPublicApiTest） |
| 内容恢复 | PASS |
| 专题下线 → 列表消失 | PASS |

---

## 9. 风险

- 真实 Admin UI / 微信开发者工具未做人工 E2E
- `topic_content` 软删后重加依赖 Service 清理再插入（H2/MP 限制）
- 专题内容上限管理端一次最多约 100 条（V1）
- 未对 Topic 做 Redis 缓存（按规范刻意不做）

---

## 10. 技术债务

- Admin `topic.test.mjs` 未加入 package.json 脚本（可手动 `node --test`）
- 拖拽排序未做（↑↓ + 保存排序）
- 专题关键词搜索仅 name LIKE

---

Sprint 07 completed.
Report: docs/sprint-07-report.md
Status: PASS
