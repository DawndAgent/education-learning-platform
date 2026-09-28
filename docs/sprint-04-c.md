# Sprint 04-C 完成报告

分类管理沿用 Sprint 02 的 Category 模块，没有第二套 Entity、Service、Mapper 或 Controller。种子分类和已有 code 没有改。小程序没有改动。内容管理、文章编辑、视频编辑没有做。

## 1. Backend

- API 沿用 `CategoryAdminController`。`GET /admin/api/categories/tree` 按 `sort ASC, id ASC` 返回整棵树。`POST`、`PUT /{id}`、`DELETE /{id}` 走原来的创建、更新、删除。
- `CategoryService` 增加两级限制：`parentId = 0` 是一级分类；父分类必须存在、未删除且启用；不能把自己或子孙设为父级；有子分类的一级分类不能改成二级。
- 删除时先拒绝仍有子分类的分类，再拒绝该分类下已有内容的分类。不级联删除子分类，也不级联删除内容。内容是否存在通过已有的 `ContentMapper` 统计。
- `adminTree`、`create`、`update`、`delete` 都有 `@RequirePermission("CATEGORY_MANAGE")`。
- 名称为空由 `@NotBlank` 拦截。code 仍是 `^[A-Z][A-Z0-9_]{1,63}$` 且唯一。
- 停用一级分类不会改子分类数据。公开树里，父级停用后子分类不再对外展示。

## 2. Admin

- 分类页是 `admin-web/src/views/categories/index.vue`，用 `el-table` 展示树，按 sort、id 升序。
- 新增一级分类时，父级可以选择「无」或某个一级分类。新增二级分类从「新增子分类」进入，父级锁定。二级行没有「新增子分类」。
- 编辑对话框回填名称、编码、父级、图标、描述、排序、状态。
- 删除使用 `ElMessageBox.confirm`，文案为 `确定删除分类「xxx」吗？`，成功后刷新树。
- 启用和停用先确认，再调用更新接口切换 `ENABLED` / `DISABLED`，成功后刷新。
- 页面只调用 `categoryApi`。首次加载失败、空列表「暂无分类」、提交中按钮 disabled / loading 都有处理。

## 3. 数据库

没有新增 migration。

Sprint 02 的 15 条种子已经覆盖这三组分类，code 保持现有值：

```text
CAMBRIDGE
CAMBRIDGE_KET
CAMBRIDGE_READING
CAMBRIDGE_LISTENING
CAMBRIDGE_ARCHIVE

MATH
MATH_EXERCISE
MATH_CONTEST
MATH_VIDEO
MATH_WEEKLY

JUNIOR
JUNIOR_GRADE_7
JUNIOR_GRADE_8
JUNIOR_GRADE_9
JUNIOR_EXAM
```

本次把后端连到 3307 上的库时，Flyway 从 v1 应用到 v3，没有再插入一份分类。

## 4. API

```text
GET    /admin/api/categories/tree
POST   /admin/api/categories
PUT    /admin/api/categories/{id}
DELETE /admin/api/categories/{id}
```

状态字段是 `ENABLED` / `DISABLED`。`id` 和 `parentId` 在 JSON 里是字符串。

## 5. 权限

`CATEGORY_MANAGE` 由后端校验。没有该权限时，侧栏不显示「分类管理」。已有接口测试覆盖无权限删除分类返回 403。

## 6. 测试

```text
mvn test：PASS，58，失败 0
mvn verify：PASS，58，失败 0
npm run build：PASS
tsc --noEmit：PASS
tsc --noEmit -p tsconfig.app.json：PASS
```

`mvn verify` 包含测试、Checkstyle 和 SpotBugs。`CategoryServiceTest` 10 个。Admin 的 `node --test` 10 个通过，其中分类 5 个、登录权限 5 个。

## 7. 真实联调

浏览器页面操作：未执行。当前没有浏览器自动化。8080 被其他 Java 服务占用，5173 被其他容器占用，没有点击 Admin 页面。

真实 MySQL + Backend 接口：PASS。MySQL 容器 `edu-learning-mysql` 映射在 3307，本项目后端临时开在 8088，使用 `admin` 登录。已完成查看种子树、新增一级、新增二级、拒绝三级、编辑、有子分类时拒绝删除、停用后公开树隐藏、再启用后公开树恢复、删除测试分类。测试数据已删除，种子仍是 3 个一级分类。联调结束后 8088 进程已停止。

## 8. 修改文件

新增：

- `admin-web/src/api/category.ts`
- `admin-web/src/types/category.ts`
- `admin-web/src/utils/category-form.ts`
- `admin-web/src/views/categories/index.vue`
- `admin-web/tests/category.test.mjs`

修改：

- `src/main/java/com/xxedu/learning/modules/category/service/CategoryService.java`
- `src/test/java/com/xxedu/learning/modules/category/CategoryServiceTest.java`
- `admin-web/src/router/index.ts`

删除：无。`miniprogram/` 未修改。

## 9. 风险

- `category.name` 列是 `VARCHAR(64)`，校验上限是 64。没有改表。
- 状态是枚举名 `ENABLED` / `DISABLED`。
- 分类 code 的唯一约束包含已逻辑删除的行，这是原有行为。
- 直接打开 `/categories` 时，前端只隐藏菜单，不拦截路由。没有权限时接口返回 403。
- Element Plus 全量引入后，构建仍有大于 500 kB 的 chunk 警告。构建本身成功。
- 库里如果已经有三级数据，管理端树可以展示，但新增和修改父级不能再往下挂。

## 10. 未实现

```text
内容管理
文章编辑
视频编辑
```

Sprint 04-C 到此停止，不进入 Sprint 04-D。
