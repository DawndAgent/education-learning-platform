# Sprint 09 完成报告

## 1. Sprint 目标

在现有 Content 发布链路上补齐运营发布管理：立即发布、定时发布、到点自动发布、取消定时、下线、重新发布、列表筛选和看板待发布数量。

不新增 `SCHEDULED` 状态。定时发布仍是 `DRAFT`，计划时间放在 `scheduled_publish_time`。

**Status: PASS**（自动化与构建通过；真实 Admin 浏览器 / 微信开发者工具 E2E NOT RUN）

---

## 2. 现状分析

Sprint 06 已有保存、立即发布、下线、重新发布、预览。Sprint 08 已开放 QUESTION / WEEKLY / DOCUMENT，发布校验按类型留在 `ContentService.assertPublishable`。

本 Sprint 扩展同一套 `publish` / `offline`，没有另写一套发布状态机。`publish_time` 仍表示最近一次正式发布时间。

---

## 3. 数据库变化

```text
V8__add_content_scheduled_publish_time.sql
```

| 变更 | 说明 |
| ---- | ---- |
| `content.scheduled_publish_time` | `DATETIME(3) NULL` |
| `idx_content_scheduled_publish_time` | 单列索引，供到期扫描 |

历史 migration 未改。状态枚举仍为 `DRAFT` / `PUBLISHED` / `OFFLINE`。

语义：

| 展示 | 数据库 |
| ---- | ------ |
| 普通草稿 | `DRAFT` 且计划时间为空 |
| 待定时发布 | `DRAFT` 且计划时间不为空 |
| 已发布 / 已下线 | 原状态；下线、立即发布、重新发布都会清空计划时间 |

已发布内容不允许再设定时发布。已下线内容设定时时，先回到 `DRAFT` 并写入计划时间。

---

## 4. Scheduler

- `ContentPublishScheduler`：`@Scheduled(fixedDelayString = "${content.publish.interval-ms:30000}")`
- 开关：`content.publish.scheduler-enabled`，环境变量 `CONTENT_PUBLISH_SCHEDULER_ENABLED`（默认 `true`）
- 测试配置关闭调度，避免测试数据被自动发布
- 每次最多 100 条：`status = DRAFT AND scheduled_publish_time <= now AND deleted = 0`
- 只查 `id`、`content_type`、`scheduled_publish_time`
- 每条单独事务。校验失败只记录 warning，不中断同批其他内容
- 真正发布前再次执行 Sprint 06 / 08 的 `assertPublishable`
- 更新条件：`UPDATE ... WHERE id = ? AND status = 'DRAFT' AND deleted = 0`。影响行数为 0 则跳过，不抛系统异常
- 成功后：`PUBLISHED`，`publish_time = now`，`scheduled_publish_time = NULL`
- 日志：`content.schedulePublish.success` / `content.schedulePublish.fail`，不打印令牌或口令

**多实例：不支持。** 当前调度适用于单实例。多实例会重复触发，需要分布式锁、ShedLock 或外部任务平台。Sprint 09 按要求未引入这些组件。

---

## 5. 定时发布

```http
POST /admin/api/contents/{id}/schedule-publish
```

请求时间格式 `yyyy-MM-dd HH:mm:ss`（也接受 ISO 本地时间）。必须晚于当前时间，否则 400。权限 `CONTENT_PUBLISH`。

调度器不是用户请求，不经过 JWT，也没有对外暴露内部发布接口。

---

## 6. 取消定时

```http
POST /admin/api/contents/{id}/cancel-scheduled-publish
```

仅 `DRAFT` 可取消。清空计划时间后仍为草稿。权限 `CONTENT_PUBLISH`。

立即发布会清空计划时间并发布。下线同样清空计划时间。

---

## 7. Admin Web

内容列表：

- 状态：全部 / 草稿 / 待定时发布 / 已发布 / 已下线（待定时发布是前端组合条件）
- 定时筛选：全部 / 普通草稿 / 待定时发布
- 发布时间：今天 / 最近 7 天 / 最近 30 天 / 自定义
- 列：发布时间、计划发布时间、更新时间
- 批量立即发布：逐条发布，返回成功数和失败数，失败条目保持原状态

编辑器（文章、视频、题目、每周一题、资料）：

- 展示当前状态、发布时间、计划发布时间
- 草稿或已下线可定时发布；已有计划时间可取消
- 已发布内容不显示定时发布
- 确认文案包含标题、类型、分类

---

## 8. Dashboard

`GET /admin/api/dashboard/overview` 增加 `scheduledPublishCount`（`DRAFT` 且计划时间不为空）。

`draftCount` 仍是全部草稿，包含待定时发布。看板上两者同时展示。

首页增加「待发布内容」，进入内容管理并带 `schedule=SCHEDULED`。

---

## 9. Mini Program

未改小程序定时逻辑，也没有 `setTimeout` / `setInterval` 判断发布时间。

未到点的内容保持草稿，公开接口不返回。到点发布后按现有 `PUBLISHED` 规则进入列表、详情和首页最新内容。

---

## 10. Topic 兼容

专题规则未改。草稿内容不出现在专题前台。定时发布成功变为 `PUBLISHED` 后，已发布专题会带出该内容。测试 `topicShowsContentOnlyAfterScheduledPublish` 覆盖这条路径。

---

## 11. 权限

定时发布、取消定时、批量立即发布使用已有 `CONTENT_PUBLISH`。下线仍用 `CONTENT_OFFLINE`。未新增 `CONTENT_SCHEDULE`。

---

## 12. 测试

| Command | Result |
| ------- | ------ |
| `mvn test` / `mvn verify` | **PASS**（122 tests） |
| `ContentSchedulePublishTest` | **PASS**（定时、取消、过期时间 400、立即发布清空计划、下线清空、并发条件更新为 0、五类内容、失败不阻断同批、批量部分失败、专题可见性、权限） |
| `admin-web` `npm run build`（含 `vue-tsc`） | **PASS** |
| `admin-web` `node --test tests/content.test.mjs` | **PASS** |
| `admin-web` `npm test` | **NOT RUN**（package.json 无该脚本） |
| Mini Program `npm run typecheck` | **PASS** |
| Mini Program `npm run test:mp` | **PASS**（48 tests） |
| 微信开发者工具编译 | **NOT RUN** |

覆盖类型：ARTICLE、VIDEO、QUESTION、WEEKLY、DOCUMENT 各完成一次定时发布并被调度任务发布。

---

## 13. E2E

| 场景 | Result |
| ---- | ------ |
| 立即发布后公开接口可见 | PASS（服务层 / 既有发布测试） |
| 定时发布前公开接口不可见，到期后可见 | PASS（`ContentSchedulePublishTest`） |
| 取消定时回到普通草稿 | PASS |
| 立即发布覆盖定时计划 | PASS |
| 下线清空计划时间 | PASS |
| 专题在内容发布后自动出现 | PASS |
| Admin 浏览器手工走查 | **NOT RUN** |
| 微信开发者工具 / 真机 | **NOT RUN** |

---

## 14. 风险

- **当前 Scheduler 只适用于单实例部署。** 多实例需要分布式锁或外部调度，本 Sprint 未做。
- 发布校验持续失败的内容会留在待发布队列，下一次调度会再试，并写 warning。没有单独的失败次数表。
- `draftCount` 包含待定时发布，和 `scheduledPublishCount` 不是互斥拆分。
- 未做批量定时发布、版本管理和审批流。
- 真实浏览器与微信 IDE E2E 未跑。
