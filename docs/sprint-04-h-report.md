# Sprint 04-H 完成报告

## 1. Sprint Summary

本 Sprint **未新增业务模块**，对 Admin Web + Backend 做了完整业务链路联调与体验收口。

结论：管理员可完成登录 → Dashboard → 分类 / 内容 → 文章 / 视频编辑 → 上传 → 草稿 / 发布 / 下线 / 删除 → Dashboard 刷新 → 退出。发现并修复了若干体验问题（错误提示刷屏、侧栏高亮、401 清会话、403 文案、登录防重复提交、404 回家路由）。

**Status: PASS**

---

## 2. Modified Files

| File | Change | Reason |
| ---- | ------ | ------ |
| `admin-web/src/utils/error-toast.ts` | 新增错误 Toast 去重工具 | 并行请求失败时避免重复弹窗 |
| `admin-web/src/api/request.ts` | 使用去重 Toast；401 清 auth store；403 专用文案 | 登录过期 / 无权限体验 |
| `admin-web/src/utils/access.ts` | 新增 `activeMenuPath` | 文章/视频编辑子路由侧栏高亮 |
| `admin-web/src/layouts/AdminLayout.vue` | 侧栏按 `activeMenuPath` 高亮 | 编辑页菜单状态正确 |
| `admin-web/src/views/error/404.vue` | 按权限跳转可访问首页 | 无 DASHBOARD_VIEW 时不硬跳 Dashboard |
| `admin-web/src/views/login/index.vue` | `loading` 期间防重复提交 | 登录重复点击 |
| `admin-web/tests/auth.test.mjs` | 侧栏高亮 / 401 用例 | 回归 |
| `admin-web/tests/polish.test.mjs` | Toast 去重与 401 清理断言 | 回归 |
| `docs/sprint-04-h-report.md` | 本报告 | Sprint 交付 |

---

## 3. Backend Changes

**无后端代码修改。**

已验证既有行为仍正确：

- `ContentService`：`contentType` 创建后锁定；`PUBLISHED` 不可直接删除
- `DashboardService`：复用 `ContentMapper` COUNT / GROUP BY，无重复统计模块
- `GlobalExceptionHandler`：统一 `ApiResponse`
- `@RequirePermission` 仍为最终权限兜底
- 公开 API 未改动

---

## 4. Admin Web Changes

- Axios 拦截器：短窗口错误去重；401 清 token + auth store 后跳转 `/login`（登录页除外）
- 403：明确提示「没有权限」或后端消息，**不**跳转登录
- 侧栏：`/articles/*`、`/videos/*` 高亮对应一级菜单
- 登录按钮防重复提交
- 404「返回首页」按当前权限选择目标页

未改动：Category / Content / Article / Video / Upload / Dashboard 核心业务实现（仅联调验证）。

---

## 5. Mini Program Compatibility

未修改 `miniprogram/`。

公开 API 实测：

| API | Result |
| --- | ------ |
| `GET /api/categories/tree` | PASS (`code=0`) |
| `GET /api/categories/1` | PASS |
| `GET /api/content` | PASS |

---

## 6. Business Flow Verification

真实 HTTP 联调（MySQL `3307` + Redis + Backend `8088`）：

| Flow | Result |
| ---- | ------ |
| Login | PASS |
| Dashboard overview | PASS |
| Dashboard after draft (+1 total / +1 draft) | PASS |
| Dashboard after publish (−1 draft / +1 published) | PASS |
| Dashboard after offline (−1 published / +1 offline) | PASS |
| Category public tree | PASS |
| Article create / publish / offline / delete | PASS |
| Video create / publish / offline / delete | PASS |
| File upload (PNG) | PASS |
| Bad token → 401 | PASS |
| Logout | PASS |
| Content type lock / delete published | PASS（请求被拒绝；PowerShell 未回显 body 文案） |

前端页面项（基于代码审查 + 既有单测，**非浏览器自动化**）：

| Item | Result |
| ---- | ------ |
| Category CRUD UX | PASS（代码 / 单测） |
| Content list filter / page reset | PASS（单测） |
| Article / Video editor submit guards | PASS（单测） |
| Upload client validation messages | PASS（单测） |
| Empty / Loading / Error 文案 | PASS（代码审查） |
| Menu permission gating | PASS（单测） |

---

## 7. Test Results

| Command | Result |
| ------- | ------ |
| `mvn test` / `mvn verify` | PASS（0 failed / 0 error） |
| `npm run build` | PASS |
| `npx tsc --noEmit` | PASS |
| `node --test`（auth / polish / dashboard / content / category / editor / file-upload） | PASS |
| `npm run lint` | **NOT RUN**（`package.json` 无 `lint` script） |

---

## 8. Browser E2E

```text
Browser E2E：未执行，原因：当前环境没有浏览器自动化能力
```

已用真实 Backend HTTP 完成主业务链路与 Dashboard 一致性验证；未伪造浏览器操作结果。

---

## 9. Bugs Fixed

### 9.1 并行请求错误 Toast 刷屏

- **问题**：页面挂载多请求同时失败时弹出多个相同 `ElMessage`
- **原因**：拦截器每次失败都 toast，无去重
- **修复**：`shouldShowErrorToast` 800ms 窗口去重
- **回归**：`tests/polish.test.mjs`

### 9.2 编辑页侧栏不高亮

- **问题**：`/articles/:id`、`/videos/create` 等路径下菜单无选中态
- **原因**：`el-menu` 直接用完整 `route.path`
- **修复**：`activeMenuPath` 映射到 `/articles` / `/videos`
- **回归**：`tests/auth.test.mjs`

### 9.3 401 只清 token 未清 auth store

- **问题**：过期后 user 仍可能残留在内存
- **原因**：拦截器只调 `clearToken`
- **修复**：同时 `useAuthStore().clearSession()`
- **回归**：`tests/polish.test.mjs` 源码断言

### 9.4 403 文案不够明确

- **问题**：无权限可能落到泛化「请求错误」
- **修复**：`status === 403` 专用「没有权限」fallback

### 9.5 登录可重复提交

- **修复**：`loading` 期间直接 return

### 9.6 404 回家硬跳 Dashboard

- **问题**：无 `DASHBOARD_VIEW` 时可能进不可见页
- **修复**：按权限选择 Dashboard / Contents / Categories / Login

---

## 10. Known Risks

- 路由层仍无按权限拦截深链；依赖菜单隐藏 + 后端 403（与 04-C/D 一致）
- 列表页 loading 失败时：拦截器 toast + 页内 `el-alert` 可能仍各提示一次（不同文案窗口下）
- Admin 刷新依赖 Vite history；生产需网关 SPA fallback（未改部署）

---

## 11. Technical Debt

- `admin-web` 无 ESLint / `npm run lint`
- 分类页按钮未按细粒度权限再藏一层（依赖菜单 + API）
- 无浏览器 E2E（Playwright/Cypress）自动化套件
- Dashboard / 列表在超大数据量下的索引优化仍可后续评估

---

## 12. Recommended Next Sprint

建议下一阶段（**不要自动开始**）可选：

1. 用户 / 角色管理（若产品需要非 ADMIN 运营账号）
2. 操作审计日志
3. 对象存储（COS）替换本地上传
4. Admin 浏览器 E2E 基建
5. 路由级权限守卫（深链体验）

---

## Acceptance Checklist

### Backend

- [x] mvn test / verify PASS
- [x] 权限 / 统一异常 / 状态流转 / 无重复 Dashboard 业务模块
- [x] 公开 API 兼容

### Admin Web

- [x] build / tsc PASS
- [x] 401 / 403 / Dashboard / Category / Content / Article / Video / Upload 链路验证
- [x] Publish / Offline / Delete / Logout 验证
- [ ] Browser E2E（环境无能力，已如实标注）

### Mini Program

- [x] 未改动；公开 API 实测 PASS
