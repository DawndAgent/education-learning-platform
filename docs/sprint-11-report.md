# Sprint 11 Report

## 1. Sprint Goal

把已有 Topic 升级成可运营的内容集合：创建专题、挂接已有 Content、排序、发布 / 下线，并在小程序专题页展示。专题不复制内容，也不变成 `content_type = TOPIC`。

现有 Sprint 07 Topic、Sprint 08 内容类型、Sprint 09 定时发布、Sprint 10 首页推荐都保留。

## 2. Implemented Features

Sprint 07 已经具备专题 CRUD、发布、下线、内容关联、排序、公开列表和详情、Admin 专题页、小程序专题列表和详情。本 Sprint 在这套实现上补齐缺口：

- 公开专题详情展示已发布的 ARTICLE、VIDEO、QUESTION、WEEKLY、DOCUMENT。原先公开 SQL 只返回文章和视频。
- 公开内容增加 `publishTime`。
- 管理端内容项返回关联 ID。
- 新增按条目添加、批量添加、按条目删除、按条目排序。批量结果返回成功、重复、无效数量，一条无效内容不会让整批失败。
- 删除专题内容只逻辑删除 `topic_content`，不删除 Content。
- 专题详情页展示内容序号。
- 添加内容后提示成功 / 重复 / 无效数量。
- 回归：首页 TOPIC 推荐仍进入已发布专题；定时草稿在到点发布后自动出现在专题页。

## 3. Database Changes

没有新的 Flyway 版本。当前最新 migration 仍是 `V9__create_home_operation_tables.sql`。

`V6__create_topic_tables.sql` 已经有：

| 对象 | 作用 |
| ---- | ---- |
| `topic` | 专题本身。标题字段是 `name`，另有 `code`、`cover_url`、`summary`、`category_id`、`status`、`sort` |
| `topic_content` | 专题与 Content 的关联，对应本 Sprint 的 TopicItem |

`topic_content` 已有 `topic_id`、`content_id`、`sort`、审计字段、`deleted`，以及唯一约束 `(topic_id, content_id, deleted)` 和 topic / content / sort 索引。

没有新建 `topic_item`，也没有把专题做成 Content。重复建表会拆出两套关联。

## 4. Backend API Changes

原有接口保持不变：

```http
GET/POST        /admin/api/topics
GET/PUT/DELETE  /admin/api/topics/{id}
POST            /admin/api/topics/{id}/publish
POST            /admin/api/topics/{id}/offline
GET/POST        /admin/api/topics/{id}/contents
DELETE          /admin/api/topics/{id}/contents/{contentId}
PUT             /admin/api/topics/{id}/contents/sort
GET             /api/topics
GET             /api/topics/{id}
```

专题标题继续用 `name`。公开列表和小程序已经按这个字段读取，改成 `title` 会破坏现有客户端。

新增：

```http
GET    /admin/api/topics/{id}/items
POST   /admin/api/topics/{id}/items
POST   /admin/api/topics/{id}/items/batch
DELETE /admin/api/topics/{id}/items/{itemId}
PUT    /admin/api/topics/{id}/items/sort
```

批量响应：

```json
{
  "successCount": 1,
  "duplicateCount": 1,
  "invalidCount": 1
}
```

排序按 `itemId` 更新，并校验该条目属于当前专题。公开详情用 JOIN 一次取出已发布内容，按 `sort ASC, id ASC`。

## 5. Admin Web Changes

专题管理仍在 `/topics`。编辑页已有基本信息、内容管理、发布确认和预览。

本 Sprint 把「添加内容」改到批量接口，并提示成功、重复、无效数量。预览仍只读，不改发布状态，并显示专题状态。上移 / 下移沿用已有排序。

## 6. Mini Program Changes

未新建专题页面。继续使用：

```text
pages/topic-list
pages/topic-detail
pages/content-detail
```

专题详情内容前增加序号。点击内容仍进入现有 `content-detail`。首页专题推荐继续走 Sprint 10 的 `GET /api/home`，类型为 TOPIC 时进入 `topic-detail`。

列表已有下拉刷新、上拉加载、空状态和错误状态。

## 7. Permission Changes

没有新增权限。Sprint 07 已有并绑定 ADMIN：

```text
TOPIC_VIEW
TOPIC_CREATE
TOPIC_UPDATE
TOPIC_DELETE
TOPIC_PUBLISH
TOPIC_OFFLINE
TOPIC_CONTENT_MANAGE
```

`TOPIC_CONTENT_MANAGE` 就是本 Sprint 的条目管理权限。添加、删除、排序都走这个权限。没有再造 `TOPIC_ITEM_MANAGE`。

缺少对应权限时接口返回 403。

## 8. Topic Publish Rules

发布沿用 `TopicService.publish`：

- 专题存在且未删除
- 名称和分类完整
- 至少一条有效内容：Content 未删除、状态 `PUBLISHED`、类型为文章 / 视频 / 题目 / 每周一题 / 资料

不满足时返回 `发布专题至少需要一篇已发布的内容`。状态仍是 `DRAFT` / `PUBLISHED` / `OFFLINE`。下线后不出现在公开专题列表。

## 9. Content Association Rules

- 允许挂接草稿和下线内容，方便先组专题再发布内容。
- 公开详情只展示 `PUBLISHED` 且未删除的内容。
- 内容下线或删除后，关联行保留；重新发布后自动出现。
- 删除专题内容只逻辑删除 `topic_content`。
- 删除 Content 不删除专题。已删除内容因 JOIN `content.deleted = 0` 从专题页消失。
- 同一专题不能重复挂接同一内容。再次添加已删除关联时，先清掉逻辑删除行再插入，避免唯一约束冲突。

## 10. Sprint 10 Compatibility

首页推荐仍使用 `home_recommendation.recommend_type = TOPIC`。`TopicAggregationTest.homeRecommendationStillOpensPublishedTopic` 确认首页返回该专题，公开详情能看到其中的已发布文章。没有重写推荐逻辑。

## 11. Sprint 09 Compatibility

定时发布的内容在到点前保持 `DRAFT`，专题页不展示。测试把计划时间拨到过去，再执行现有 `ContentPublishJob`。内容变为 `PUBLISHED` 后，同一条 `topic_content` 自动出现在专题详情。没有复制内容，也没有改调度器。

## 12. Test Results

| Command | Result |
| ------- | ------ |
| `mvn verify` | **PASS**（134 tests，0 failure） |
| `TopicAggregationTest` | **PASS**（7） |
| `TopicServiceTest` / `TopicPublicApiTest` / `HomeOperationTest` | **PASS** |
| `admin-web` `npm run build`（含 `vue-tsc`） | **PASS** |
| `admin-web` `node --test tests/*.mjs` | **PASS**（35） |
| Mini Program `npm run typecheck` | **PASS** |
| Mini Program `npm run test:mp` | **PASS**（48） |
| 微信开发者工具编译 | **NOT RUN** |

## 13. Integration Test Results

测试库是项目既有 H2，不是一套独立 MySQL。`TopicAggregationTest` 覆盖：

| 场景 | Result |
| ---- | ------ |
| 创建专题，加入文章和视频，发布后公开接口同时看到两者 | **PASS** |
| 视频下线后公开详情消失，重新发布后恢复 | **PASS** |
| 定时草稿先隐藏，调度任务发布后出现 | **PASS** |
| 批量添加统计重复和无效 ID | **PASS** |
| 按条目删除不删除文章；跨专题排序被拒绝 | **PASS** |
| 已发布每周一题出现在公开专题；删除内容后专题还在、内容隐藏 | **PASS** |
| 首页 TOPIC 推荐指向该专题详情 | **PASS** |
| 无创建 / 编辑 / 删除 / 发布权限返回 403 | **PASS** |
| Admin 浏览器手工走查 | **NOT RUN** |
| 微信开发者工具点击专题到内容详情 | **NOT RUN** |

## 14. Modified Files

- `src/main/java/com/xxedu/learning/modules/topic/service/TopicService.java`
- `src/main/java/com/xxedu/learning/modules/topic/controller/TopicAdminController.java`
- `src/main/resources/mapper/topic/TopicContentMapper.xml`
- `src/main/java/com/xxedu/learning/modules/topic/mapper/TopicContentRow.java`
- `src/main/java/com/xxedu/learning/modules/topic/vo/PublicTopicContentVO.java`
- `src/main/java/com/xxedu/learning/modules/topic/vo/TopicContentItemVO.java`
- `src/main/java/com/xxedu/learning/modules/topic/dto/TopicItemAddRequest.java`
- `src/main/java/com/xxedu/learning/modules/topic/dto/TopicItemBatchRequest.java`
- `src/main/java/com/xxedu/learning/modules/topic/dto/TopicItemSortRequest.java`
- `src/main/java/com/xxedu/learning/modules/topic/vo/TopicItemBatchResultVO.java`
- `src/test/java/com/xxedu/learning/modules/topic/TopicAggregationTest.java`
- `admin-web/src/api/topic.ts`
- `admin-web/src/types/topic.ts`
- `admin-web/src/views/topics/editor.vue`
- `miniprogram/pages/topic-detail/topic-detail.ts`
- `miniprogram/pages/topic-detail/topic-detail.wxml`
- `miniprogram/pages/topic-detail/topic-detail.wxss`
- `miniprogram/types/topic.ts`

## 15. Known Issues

- 集成测试跑在 H2，没有另接真实 MySQL。
- 真实 Admin 浏览器和微信开发者工具走查未执行。
- 公开专题列表排序是 `sort ASC, publish_time DESC, id DESC`。这是 Sprint 07 的既有顺序，本 Sprint 没有改成只按 `id DESC`，避免打乱首页精选。
- 专题字段名是 `name`，不是 `title`。
- 关联表名是 `topic_content`，不是 `topic_item`。新接口使用 `/items`，旧 `/contents` 仍可用。

## 16. Deferred Items

- Redis 专题缓存
- 专题全文搜索
- 付费、会员、学习进度、收藏、评论
- 拖拽排序
- 把专题改造成 Content 类型

## 17. Final Conclusion

PASS WITH KNOWN ISSUES

专题聚合、发布、内容关联、首页推荐和定时发布兼容均已由现有模块加增量接口完成，自动化测试通过。已知限制是测试库为 H2，以及浏览器 / 微信开发者工具实机走查未执行。
