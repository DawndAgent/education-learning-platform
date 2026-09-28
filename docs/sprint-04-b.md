# Sprint 04-B 完成报告

管理员登录接在现有的 `sys_user`、角色权限表和 JWT 上。小程序没有改动。分类、内容、文章、视频的增删改，以及角色管理和用户管理页面，都没有做。

## 1. Backend

- `POST /admin/api/auth/login` 校验用户名、BCrypt 密码和账号状态，签发已有的 JWT。
- `GET /admin/api/auth/me` 按 Token 从数据库读取当前管理员、昵称和权限。
- `POST /admin/api/auth/logout` 只要求已登录并返回成功。没有引入 Redis 黑名单，退出由前端删除 Token 完成。
- 登录接口允许匿名。其余 `/admin/api/**` 必须携带有效 JWT。
- 权限仍走已有的 `@RequirePermission`。没有 `CONTENT_DELETE` 时删除内容返回 403。没有 `CATEGORY_MANAGE` 时删除分类返回 403。

## 2. Database

新增 migration：`V3__seed_admin_user.sql`。

没有新建表。复用 Sprint 01 的：

- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `sys_role_permission`

初始化数据：

- 角色 `ADMIN`，id 为 1
- 用户 `admin`，昵称“管理员”，状态启用，密码是 BCrypt 密文
- 该用户绑定 `ADMIN` 角色
- 角色绑定已有的 8 个权限

本地开发账号只写在 `admin-web/README.md`：用户名 `admin`，密码 `Admin@123456`。仓库里没有明文密码。

## 3. Admin Web

- 登录页使用 Element Plus 表单，用户名和密码不能为空，登录中有 loading，失败有提示，成功进入 `/dashboard`。
- `stores/auth.ts` 负责 token、用户、权限、登录、退出、刷新后拉取 `/me`、`hasPermission` 和 `hasAnyPermission`。
- Token 存在 `localStorage` 的 `admin_token`。不保存密码。
- 未登录访问后台会去 `/login`。已登录再打开 `/login` 会去 `/dashboard`。刷新后用 Token 调用 `/me`。
- Axios 自动带上 `Authorization: Bearer {token}`。HTTP 401 会清除 Token 并回到登录页。登录接口自己的 401 不会来回跳转。
- 顶栏显示昵称和“退出登录”。
- 菜单按权限显示：内容管理和文章管理需要 `CONTENT_VIEW`，分类管理需要 `CATEGORY_MANAGE`，视频管理需要 `VIDEO_MANAGE`。首页始终显示。

## 4. API

```text
POST /admin/api/auth/login
GET  /admin/api/auth/me
POST /admin/api/auth/logout
```

登录成功返回 `token`、`tokenType`、`expiresIn` 和用户信息。用户 id 按现有约定序列化成字符串。`expiresIn` 是 7200 秒。

## 5. 权限

```text
CONTENT_VIEW
CONTENT_CREATE
CONTENT_UPDATE
CONTENT_DELETE
CONTENT_PUBLISH
CONTENT_OFFLINE
CATEGORY_MANAGE
VIDEO_MANAGE
```

默认管理员拥有以上全部权限。前端只控制菜单显示，接口是否允许仍由后端判断。

## 6. 测试

```text
mvn test：PASS
mvn verify：PASS
npm run build：PASS
tsc：PASS
```

`mvn verify` 包含测试、Checkstyle 和 SpotBugs。后端测试 54 个，0 失败。其中认证相关 10 个，覆盖登录成功、用户不存在、密码错误、账号禁用、当前用户、无 Token、无效 Token、过期 Token、无删除权限 403、无分类管理权限 403。

管理端规则测试 5 个，0 失败，覆盖路由守卫、登录接口 401、菜单权限和 Token 存储。没有对着运行中的后端在浏览器里完成一次真实登录。

## 7. 修改文件

新增：

- `src/main/resources/db/migration/V3__seed_admin_user.sql`
- `src/main/java/com/xxedu/learning/modules/auth/entity/SysUser.java`
- `src/main/java/com/xxedu/learning/modules/auth/mapper/SysUserMapper.java`
- `src/main/resources/mapper/auth/SysUserMapper.xml`
- `src/main/java/com/xxedu/learning/modules/auth/dto/LoginRequest.java`
- `src/main/java/com/xxedu/learning/modules/auth/vo/CurrentUserVO.java`
- `src/main/java/com/xxedu/learning/modules/auth/vo/LoginResponse.java`
- `src/main/java/com/xxedu/learning/modules/auth/service/AuthService.java`
- `src/main/java/com/xxedu/learning/modules/auth/controller/AuthController.java`
- `src/test/java/com/xxedu/learning/modules/auth/AuthApiTest.java`
- `admin-web/src/api/auth.ts`
- `admin-web/src/stores/auth.ts`
- `admin-web/src/types/auth.ts`
- `admin-web/src/utils/token.ts`
- `admin-web/src/utils/access.ts`
- `admin-web/src/utils/permission.ts`
- `admin-web/src/views/login/index.vue`
- `admin-web/tests/auth.test.mjs`

修改：

- `src/main/java/com/xxedu/learning/common/constant/ApiConstants.java`
- `src/main/java/com/xxedu/learning/security/SecurityConfig.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/test/java/com/xxedu/learning/foundation/FlywayMigrationTest.java`
- `admin-web/src/api/request.ts`
- `admin-web/src/router/index.ts`
- `admin-web/src/layouts/AdminLayout.vue`
- `admin-web/README.md`

删除：无。`miniprogram/` 未修改。

## 8. 风险

- JWT 仍是无状态的。退出登录后，旧 Token 在过期前依然有效。
- Token 放在 `localStorage`，页面如果出现 XSS，Token 可能被读走。
- V3 会在所有执行该 migration 的环境创建同一个开发账号。生产环境部署后必须立刻修改密码。
- 开发环境 JWT 密钥仍是 `application-dev.yml` 里的默认值。生产环境必须通过 `JWT_SECRET` 注入，不能使用这个默认值。

## 9. 未实现

```text
分类 CRUD
内容 CRUD
文章编辑
视频编辑
角色管理
用户管理
```

均不属于 Sprint 04-B。

Sprint 04-B 到此停止，不进入 Sprint 04-C。
