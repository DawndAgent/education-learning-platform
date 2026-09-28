# Sprint 04-D 完成报告

内容管理沿用 Sprint 02 的 Content 模块，补上了主表的新增、编辑，以及发布、删除时缺的校验。没有第二套 Content 模块。小程序没有改动。文章正文和视频详情没有做。

## 1. Backend

- 查询、分页、搜索、筛选继续用 `GET /admin/api/contents`。支持 `pageNum`、`pageSize`、`keyword`（只匹配标题）、`categoryId`（含子孙分类）、`contentType`、`status`。排序仍是分类 `sort`、分类 `id`、内容 `sort`、`publish_time DESC`、`id DESC`。
- `POST /admin/api/contents` 只写内容主表，状态固定为 `DRAFT`。只允许 `ARTICLE` 和 `VIDEO`，并且只能挂到已启用的二级分类。
- `PUT /admin/api/contents/{id}` 只改标题、分类、封面、摘要、排序。请求里即使带了 `status` 也不会改状态。内容类型创建后不能改。
- 删除使用已有逻辑删除。已发布内容返回「请先下架内容后再删除」。草稿和已下架可以删除，删除后管理端和公开接口都不再返回。
- 发布和下架沿用原来的 `publish`、`offline`。发布前检查标题、分类、类型，分类必须存在、未删除且启用。只有内容主表、还没有视频详情的视频也可以发布；已经有视频行时，仍会同步视频状态。
- 查询 `CONTENT_VIEW`，新增 `CONTENT_CREATE`，编辑 `CONTENT_UPDATE`，删除 `CONTENT_DELETE`，发布 `CONTENT_PUBLISH`，下架 `CONTENT_OFFLINE`。
- 分类名称按本页 id 批量查询一次，不是逐条查分类。

## 2. Admin

内容页是 `admin-web/src/views/contents/index.vue`。

- 列表展示标题、分类名、类型（文章/视频）、状态标签（草稿/已发布/已下架）、排序、浏览量、收藏数、发布时间、更新时间。
- 关键词、分类、类型、状态可以搜索和重置。分类下拉按一级、二级缩进。新增和编辑只能选二级分类。
- 分页默认 10 条，可选 10、20、50。搜索或改每页条数时回到第 1 页。空列表显示「暂无内容」。
- 新增保存为草稿。编辑时内容类型不可改。
- 草稿显示编辑、发布、删除。已发布显示编辑、下架。已下架显示编辑、发布、删除。没有对应权限时按钮不显示。
- 删除、发布、下架都先确认。接口错误由现有请求拦截器用 `ElMessage.error` 显示。

## 3. API

```text
GET    /admin/api/contents
GET    /admin/api/contents/{id}
POST   /admin/api/contents
PUT    /admin/api/contents/{id}
DELETE /admin/api/contents/{id}
POST   /admin/api/contents/{id}/publish
POST   /admin/api/contents/{id}/offline
GET    /admin/api/categories/tree
```

## 4. 数据库

没有新增 migration，也没有改已有表。标题上限沿用现有列 `VARCHAR(128)`。

## 5. 权限

```text
CONTENT_VIEW
CONTENT_CREATE
CONTENT_UPDATE
CONTENT_DELETE
CONTENT_PUBLISH
CONTENT_OFFLINE
```

后端按接口校验。前端只隐藏按钮。

## 6. 测试

```text
mvn test：PASS，62，失败 0
mvn verify：PASS，62，失败 0
npm run build：PASS
tsc --noEmit：PASS
tsc --noEmit -p tsconfig.app.json：PASS
```

`mvn verify` 包含测试、Checkstyle 和 SpotBugs。`ContentServiceTest` 8 个。Admin 的 `node --test` 15 个通过，其中内容页 5 个。

标题搜索的转义符改成 `!` 之后，又完整跑过一次 `mvn verify`。

## 7. 真实联调

浏览器页面操作：未执行。没有浏览器自动化。8080 被其他 Java 服务占用，5173 被其他容器占用，没有点击内容管理页。

真实 MySQL + Backend 接口：PASS。MySQL 在 3307，后端临时开在 8088，使用 `admin` 登录。实际做过：空列表、新增文章草稿、新增视频草稿、编辑标题和二级分类、发布、已发布时拒绝删除、下架后再删除、标题关键词、下划线按字面匹配、`%` 不会变成通配、分类加类型加状态筛选、拒绝挂到一级分类、拒绝 `QUESTION`、发布后公开列表能查到、下架后公开列表不再返回、删除后管理端详情 404。测试数据已删完，内容总数回到 0。联调结束后 8088 进程已停止。

## 8. 修改文件

新增：

- `src/main/java/com/xxedu/learning/modules/content/dto/ContentCreateRequest.java`
- `src/main/java/com/xxedu/learning/modules/content/dto/ContentUpdateRequest.java`
- `admin-web/src/api/content.ts`
- `admin-web/src/types/content.ts`
- `admin-web/src/utils/content-form.ts`
- `admin-web/src/views/contents/index.vue`
- `admin-web/tests/content.test.mjs`

修改：

- `src/main/java/com/xxedu/learning/modules/content/service/ContentService.java`
- `src/main/java/com/xxedu/learning/modules/content/controller/ContentAdminController.java`
- `src/main/java/com/xxedu/learning/modules/content/convert/ContentConverter.java`
- `src/main/java/com/xxedu/learning/modules/content/vo/ContentListVO.java`
- `src/main/java/com/xxedu/learning/modules/content/vo/ContentDetailVO.java`
- `src/main/resources/mapper/content/ContentMapper.xml`
- `src/main/java/com/xxedu/learning/modules/category/service/CategoryService.java`
- `src/test/java/com/xxedu/learning/modules/content/ContentServiceTest.java`
- `admin-web/src/router/index.ts`

删除：无。`miniprogram/` 未修改。

## 9. 风险

- 标题数据库上限是 128，不是任务说明里的 200。没有改表。
- 原来的 `LIKE ... ESCAPE '\'` 在 MySQL 上会语法错误并返回 500，H2 测试发现不了。转义符已改为 `!`，标题里的 `%` 和 `_` 按字面搜索。
- 只有内容主表的视频可以发布。视频播放地址要到后续视频编辑才会有。
- 后端如果没传 `pageSize`，默认仍是 20。管理端页面固定传 10、20 或 50。
- 直接打开 `/contents` 时，前端不拦截路由。没有 `CONTENT_VIEW` 时接口返回 403。
- Element Plus 全量引入后，构建仍有大于 500 kB 的 chunk 警告。构建本身成功。

## 10. 未实现

```text
文章编辑器
视频编辑器
二维码
视频上传
文件上传
```

文章正文、视频播放、对象存储也没有做。Sprint 04-D 到此停止，不进入 Sprint 04-E。
