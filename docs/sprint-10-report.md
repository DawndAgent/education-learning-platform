# Sprint 10 完成报告

## 1. Sprint 目标

管理员在 Admin Web 配置 Mini Program 首页展示，不改 Content 核心模型。

首页结构由代码固定：

```text
Banner → 一级栏目 → 重点推荐 → 专题精选 → 最新内容
```

Banner、重点推荐、专题精选来自运营配置。栏目仍是启用的一级 Category。最新内容仍按发布时间倒序。专题精选复用 `home_recommendation` 中 `recommend_type = TOPIC`，不另建专题关系，也不再查一遍专题推荐。

**Status: PASS**（自动化与构建通过；真实 Admin 浏览器 / 微信开发者工具 E2E NOT RUN）

---

## 2. 数据库变化

```text
V9__create_home_operation_tables.sql
```

历史 migration 未改。V8 仍是定时发布时间。

| 表 | 说明 |
| -- | ---- |
| `home_banner` | 标题、副标题、图片、跳转、排序、状态、起止时间、审计字段、逻辑删除 |
| `home_recommendation` | 类型 CONTENT / TOPIC、目标 ID、排序、状态、逻辑删除 |

索引：

```text
idx_banner_status
idx_banner_time (start_time, end_time)
idx_banner_sort
idx_banner_deleted
idx_recommend_type
idx_recommend_status
idx_recommend_sort
idx_recommend_deleted
uk_recommend_target (recommend_type, target_id, deleted)
```

权限种子：`HOME_OPERATION_VIEW`（130）、`HOME_OPERATION_MANAGE`（131），绑定 ADMIN 角色（330、331）。权限总数 17 → 19。

---

## 3. Banner

管理接口：

```http
GET    /admin/api/home/banners
POST   /admin/api/home/banners
PUT    /admin/api/home/banners/{id}
DELETE /admin/api/home/banners/{id}
POST   /admin/api/home/banners/{id}/enable
POST   /admin/api/home/banners/{id}/disable
PUT    /admin/api/home/banners/sort
```

- 跳转类型：`CONTENT`、`TOPIC`、`URL`、`NONE`。CONTENT / TOPIC 必须有目标且目标存在，允许目标尚未发布。URL 必须是 http(s)。NONE 不要求目标。
- 新建默认 `DISABLED`，启用后才进入首页。
- 删除在同一事务里先停用再逻辑删除。
- 图片复用现有上传组件，场景 `COVER`。建议 750×300，后端不校验像素。
- 列表支持关键字、状态、分页。
- 排序 `sort ASC, id DESC`。上移 / 下移后一次提交。

前台条件：`ENABLED`，且开始时间为空或已到，结束时间为空或未过。CONTENT / TOPIC 目标不是 `PUBLISHED` 时跳过。最多 5 条。时间是查询条件，没有定时任务。

---

## 4. Recommendation

管理接口：

```http
GET    /admin/api/home/recommendations
POST   /admin/api/home/recommendations
DELETE /admin/api/home/recommendations/{id}
PUT    /admin/api/home/recommendations/sort
```

- 类型只有 `CONTENT`、`TOPIC`。
- 添加时目标必须存在，允许 `OFFLINE`，便于提前配置。
- 同一类型 + 目标不能有两条未删除记录。移除后再添加会恢复逻辑删除行，避免 `(recommend_type, target_id, deleted)` 上出现第二条 `deleted = 1`。
- 删除先停用再逻辑删除。
- 新建默认 `ENABLED`。
- 前台只返回目标为 `PUBLISHED` 的记录，最多 6 条。下线或删除后自动隐藏，重新发布后自动恢复，不用重新配置。

专题精选就是这 6 条里的 TOPIC，由同一次查询在内存中过滤，不第二次查库。

---

## 5. Home API

```http
GET /api/home
```

无需 JWT。Admin 预览和小程序都调用 `HomeQueryService.load(now)`。

```json
{
  "banners": [],
  "categories": [],
  "recommendations": [],
  "topics": [],
  "latestContents": []
}
```

| 字段 | 规则 |
| ---- | ---- |
| banners | 有效 Banner，最多 5 |
| categories | 启用的一级分类 |
| recommendations | 有效推荐，最多 6，含 CONTENT 与 TOPIC |
| topics | `recommendations` 中 `type = TOPIC` 的内存子集 |
| latestContents | `PUBLISHED`，`publish_time DESC`，最多 10 |

候选先按排序取最多 50 条，过滤无效目标后再截断，避免无效行占满名额。内容和专题批量查询，不逐条查。无效推荐被跳过，不让整个接口 500。响应不含 `created_by`、`updated_by`、`deleted` 和内部路径。

未加 Redis 首页缓存。

---

## 6. Admin Web

菜单「首页运营」在专题管理之后，路径 `/home-operation`。进入需要 `HOME_OPERATION_VIEW` 或 `HOME_OPERATION_MANAGE`。写操作需要 `HOME_OPERATION_MANAGE`。

页面三块：

- Banner 管理：封面、标题、跳转、排序、状态、时间；编辑、启用、停用、删除、上移、下移、保存排序。
- 重点推荐：类型、标题、封面、分类、排序、目标状态；添加、移除、上移、下移、保存排序。添加时默认可搜索已发布内容和专题，也可以改筛选以便提前配置未发布目标。
- 首页预览：调用 `GET /api/home`，展示 Banner、栏目、重点推荐、专题精选、最新内容。

---

## 7. Mini Program

首页 `pages/index` 改为一次 `GET /api/home`，保留下拉刷新。没有本地永久缓存。

- Banner：内容进现有 `content-detail`，专题进现有 `topic-detail`。外部 URL 不打开 web-view，复制链接并提示需要配置小程序业务域名。`NONE` 不可点。
- 栏目：仍进现有分类页，再进入内容列表。
- 重点推荐只展示 `CONTENT`。专题精选展示 `topics`。
- 没有 Banner、推荐或专题时隐藏对应区块，不让首页失败。没有最新内容时显示空状态。接口失败显示「首页加载失败」和「重新加载」。
- 「更多」进入现有搜索页。内容列表页要求分类编号，全站最新内容不能无分类打开 `content-list`。

未新增详情页，也没有为拆组件重写首页。栏目卡片和内容卡片继续复用。

---

## 8. 权限

| 能力 | 权限 |
| ---- | ---- |
| Banner / 推荐读写 | `HOME_OPERATION_MANAGE` |
| 列表与首页预览页 | `HOME_OPERATION_VIEW`（管理权限也可进入页面） |
| `GET /api/home` | 无 JWT |

ADMIN 由 V9 种子获得这两个权限。

---

## 9. 测试

| Command | Result |
| ------- | ------ |
| `mvn verify` | **PASS**（127 tests，0 failure） |
| `HomeOperationTest` | **PASS**（5） |
| `admin-web` `npm run build`（含 `vue-tsc`） | **PASS** |
| `admin-web` `node --test tests/*.mjs` | **PASS**（35） |
| `admin-web` `npm test` | **NOT RUN**（package.json 无该脚本） |
| Mini Program `npm run typecheck` | **PASS** |
| Mini Program `npm run test:mp` | **PASS**（48） |
| 微信开发者工具编译 | **NOT RUN** |

`HomeOperationTest` 覆盖：Banner 创建、启停、删除、排序、时间窗口；推荐创建、重复、删除后再添加、排序；已发布内容 / 专题可见，下线后隐藏，重新发布后恢复；Banner 最多 5、推荐最多 6。时间通过 `HomeQueryService.load(固定时间)` 传入，没有改系统时钟。

---

## 10. E2E

| 场景 | Result |
| ---- | ------ |
| 启用 Banner 出现，停用后消失，再启用恢复 | PASS（`HomeOperationTest`） |
| 未开始 / 已过期 Banner 不返回 | PASS |
| 已发布推荐可见，下线隐藏，重新发布恢复 | PASS |
| 专题下线隐藏，重新发布恢复 | PASS |
| 排序与数量上限 | PASS |
| Admin 登录后在浏览器创建 Banner 和推荐 | **NOT RUN** |
| 微信开发者工具看到首页并点击详情 | **NOT RUN** |
| 真实时间跨过 Banner 开始时间 | **NOT RUN**（用服务入参模拟） |

---

## 11. 风险

- 首页运营和公开首页共用一套组装。预览不是第二套业务逻辑，但它依赖公开接口，后台登录态会带上 JWT，接口本身不校验登录。
- 重点推荐和专题精选共享最多 6 个名额。TOPIC 多时，内容推荐名额会变少。
- 推荐唯一约束包含 `deleted`。移除后再次添加会恢复原逻辑删除行。不要再物理删除这些行。
- 小程序外部链接只复制，不内嵌网页。未配置业务域名时用户不能在小程序内打开。
- 「更多」进入搜索页，而不是无分类的 `content-list`。
- 未做首页缓存。运营变更会立刻反映到下一次查询，数据量上来后再考虑缓存。
- 真实浏览器和微信 IDE 未跑。

---

## 12. 未执行测试

| 项 | Result |
| -- | ------ |
| 微信开发者工具编译与预览 | **NOT RUN** |
| Admin 浏览器手工走查 | **NOT RUN** |
| 小程序真机 / 模拟器点击 Banner、内容、专题 | **NOT RUN** |
| 等待真实时钟进入 Banner 时间窗 | **NOT RUN** |

完成项：Home Banner、Banner CRUD、启停、排序、时间控制、重点推荐、推荐增删与排序、内容推荐、专题推荐、Home API、无效数据过滤、数量限制、Admin 首页运营、Banner UI、推荐 UI、首页预览、小程序首页改造、点击跳转、下拉刷新、空状态、错误状态、权限、Flyway、集成测试、Admin 构建、小程序 TypeScript 编译、`docs/sprint-10-report.md`。

真实端到端走查未执行。
