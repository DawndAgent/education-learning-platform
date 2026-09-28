# Sprint 04-G 完成报告

Admin 首页已接入真实运营看板：统计来自 `content` / `category` 业务表，无独立统计库、无 Redis 缓存、无定时任务。管理员登录后可看到内容状态、分类分布、最近发布与权限控制的快捷入口。

## 1. 完成内容

### Dashboard API

- `GET /admin/api/dashboard/overview`
- `DashboardController` → `DashboardService` → 复用 `ContentMapper` 统计 SQL
- VO：`DashboardOverviewVO` / `DashboardCategoryStatVO` / `DashboardRecentContentVO`
- 统计字段：内容总数、已发布 / 草稿 / 已下线、文章 / 视频、本周 / 本月新增、一级分类分布、最近 10 条已发布

### 统计卡片 + 分类 + 最近发布 + 快捷入口

- 改写 `admin-web/src/views/dashboard/index.vue`
- 欢迎语取当前登录用户，日期取浏览器本地日期
- 分类：列表 + `el-progress`（未引入 ECharts）
- 最近发布：`el-table`，查看跳转 `/articles/{id}` 或 `/videos/{id}`
- 快捷操作按权限显示：`CONTENT_CREATE` / `CONTENT_VIEW` / `CATEGORY_MANAGE`
- loading：`el-skeleton`；error：失败提示 + 重新加载；empty：暂无内容数据；支持手动刷新

### 权限

- 新增 `DASHBOARD_VIEW`
- Flyway `V5__add_dashboard_view_permission.sql`
- ADMIN 角色默认拥有
- 菜单 `/dashboard` 也要求 `DASHBOARD_VIEW`

## 2. API

```text
GET /admin/api/dashboard/overview
```

Request：

```text
Authorization: Bearer <token>
```

Response（节选）：

```json
{
  "code": "0",
  "data": {
    "contentTotal": 3,
    "publishedCount": 2,
    "draftCount": 1,
    "offlineCount": 0,
    "articleCount": 2,
    "videoCount": 1,
    "weekNewCount": 3,
    "monthNewCount": 3,
    "categoryStats": [
      { "categoryId": "1", "categoryName": "剑桥英语", "count": 2 }
    ],
    "recentPublished": [
      {
        "id": "2103015359014637570",
        "title": "直播看板视频",
        "contentType": "VIDEO",
        "categoryName": "KET/PET备考资料",
        "publishTime": "2026-09-24T14:55:08.45"
      }
    ]
  }
}
```

Permission：`DASHBOARD_VIEW`（无权限 403）

说明：

- ID（`categoryId` / recent `id`）继续按项目约定序列化为字符串
- 计数字段使用 `long`，JSON 为数字，避免被全局 `Long → String` 序列化影响

## 3. SQL / 数据库

使用表：

- `content`（主统计）
- `category`（JOIN 名称；一级分类按 `parent_id = 0` 上卷）

统计定义：

| 指标 | 条件 |
|------|------|
| contentTotal | `deleted = 0`（由 status 三项之和） |
| published / draft / offline | `deleted = 0` + 对应 `status` |
| article / video | `deleted = 0` + `content_type` |
| week / month | `deleted = 0` + `created_at` 半开区间 |
| categoryStats | 仅一级分类，子类内容计入父级 |
| recentPublished | `PUBLISHED` + `deleted = 0`，`publish_time DESC`，`LIMIT 10`，JOIN category |

时区：与 `application.yml` / JDBC 一致，使用 `Asia/Shanghai` 计算本周（周一 00:00）与本月区间。

新增索引：无（当前数据量小，沿用 `idx_content_category_status` / `idx_content_type_status`）。

新增 Flyway：`V5__add_dashboard_view_permission.sql`。

未创建：`dashboard_stat` 等统计表。

## 4. 权限

- Migration：`V5` 插入 `sys_permission.id=110` / `DASHBOARD_VIEW`
- `sys_role_permission`：`role_id=1` → `permission_id=110`
- `PermissionCodes.DASHBOARD_VIEW`
- ADMIN 登录权限数：10

## 5. 测试

```text
mvn test / mvn verify     PASS（0 failed / 0 error）
npm run build             PASS
npx tsc --noEmit          PASS
node --test dashboard/auth PASS
```

后端覆盖：

- 无 `DASHBOARD_VIEW` → 403
- 含 ARTICLE/VIDEO、DRAFT/PUBLISHED/OFFLINE、DELETED 时统计正确
- 一级分类聚合、最近发布排序
- Flyway V5 与权限种子

前端覆盖：

- 菜单权限、快捷入口权限、编辑跳转、API 分层

## 6. 真实集成

已执行：

```text
MySQL (edu-learning-mysql:3307)
Redis (6379)
Backend (8088)
Admin Login (admin / Admin@123456)
GET /admin/api/dashboard/overview
```

验证结果：

- 登录后 permissions 含 `DASHBOARD_VIEW`（共 10 项）
- 空库时全 0 / 空列表
- 创建 2 文 + 1 视频并发布后：`contentTotal=3`、`published=2`、`draft=1`、分类上卷正确、最近发布 2 条
- 测试数据已清理（下线后删除）

## 7. 浏览器验证

```text
Browser E2E was not executed because no browser automation capability was available.
```

已通过 HTTP 接口完成登录与 Dashboard 数据校验。

## 8. 性能

```text
Not benchmarked.
```

开发库规模下单次 overview 查询在集成测试中约十余毫秒量级；未做正式压测，未加缓存。

## 9. 风险

- 内容量很大时，按 `created_at` / `status` 的 GROUP BY 可能变慢，可再评估 `(deleted, created_at)` 等组合索引
- 分类统计依赖「最多两级」；更深层级需扩展上卷逻辑
- 未来若 Dashboard QPS 升高，再考虑短 TTL 缓存；本 Sprint 刻意不做

## 10. 未改动

- `miniprogram/`
- 用户 / 学习 / 收藏 / 评论行为分析
- Redis Dashboard Cache / `@Scheduled` / 消息队列 / ECharts
