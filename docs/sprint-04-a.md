# Sprint 04-A 完成报告

`admin-web` 原先不存在，已在仓库根目录新建 Vue 3 管理后台基础工程。后端和小程序没有改动，也没有新增业务接口。开发服务本次在 `http://127.0.0.1:5174/`，因为 `5173` 已被占用。

## 1. 完成内容

* Vue 3 + TypeScript + Vite 工程
* Element Plus（中文语言包，全局注册）
* Vue Router
* Pinia
* Axios 统一实例：`baseURL`、`timeout`、请求拦截器、响应拦截器
* AdminLayout
* 左侧菜单：首页、内容管理、分类管理、文章管理、视频管理
* 顶栏：教育内容管理平台、管理员
* Dashboard：内容统计占位，五项数字均为 `0`
* 分类、内容、文章、视频四个占位页
* 404 页面，返回首页进入 `/dashboard`

Axios 会区分网络错误、HTTP 4xx、HTTP 5xx 和业务码非 `"0"`。本阶段没有接入登录 Token。

## 2. 修改文件

新增：

* `admin-web/package.json`
* `admin-web/package-lock.json`
* `admin-web/index.html`
* `admin-web/vite.config.ts`
* `admin-web/tsconfig.json`
* `admin-web/tsconfig.app.json`
* `admin-web/tsconfig.node.json`
* `admin-web/README.md`
* `admin-web/.gitignore`
* `admin-web/.env.development`
* `admin-web/.env.production`
* `admin-web/src/main.ts`
* `admin-web/src/App.vue`
* `admin-web/src/style.css`
* `admin-web/src/env.d.ts`
* `admin-web/src/api/request.ts`
* `admin-web/src/layouts/AdminLayout.vue`
* `admin-web/src/router/index.ts`
* `admin-web/src/stores/app.ts`
* `admin-web/src/types/api.ts`
* `admin-web/src/views/dashboard/index.vue`
* `admin-web/src/views/placeholder/index.vue`
* `admin-web/src/views/error/404.vue`
* `admin-web/src/components/.gitkeep`

脚手架自带的 `HelloWorld.vue` 已删除。`backend` 与 `miniprogram` 未修改。

## 3. 路由

```text
/dashboard    首页
/categories   分类管理占位
/contents     内容管理占位
/articles     文章管理占位
/videos       视频管理占位
/404          页面不存在
```

`/` 会转到 `/dashboard`。未知地址会转到 `/404`。

## 4. 验证结果

```text
npm install：PASS
npm run build：PASS
tsc：PASS
页面启动：PASS
```

`npx tsc --noEmit` 和 `npx tsc --noEmit -p tsconfig.app.json` 均为 0。`npm run build` 中的 `vue-tsc -b` 通过。

开发服务已启动。以下地址均返回 HTTP 200，页面包含应用根节点：

```text
/dashboard
/categories
/contents
/articles
/videos
/404
```

当前环境没有浏览器工具，没有在 1920×1080、1440×900、1366×768 下做点击和截图。布局按固定顶栏、固定侧栏、主区域滚动来写，并隐藏了页面级横向滚动。

## 5. 问题

* 本机 `5173` 已被其他程序占用，这次开发服务实际端口是 `5174`。
* 生产包里 Element Plus 全量样式使主包超过 500 kB，构建有体积警告，构建本身成功。
* 三个桌面分辨率下的实际排版还没有在浏览器里点过。

## 6. 未实现内容

```text
登录
权限
CRUD
文章编辑
视频编辑
```

均未实现。Dashboard 数字是前端占位 `0`，没有新增统计接口。

Sprint 04-A 到此停止，等待下一阶段指令。
