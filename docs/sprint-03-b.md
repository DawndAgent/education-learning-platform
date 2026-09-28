# Sprint 03-B 完成报告

首页会按后端分类树展示三个一级板块，分类页展示对应的二级分类。没有进入 03-C，也没有改后端。

## 1. 完成内容

首页从 `GET /api/categories/tree` 取一级分类。判断条件是 `parentId` 为 `"0"`，再按 `sort` 排序，不再写死分类编号。卡片副标题来自该分类的子分类名称。

最新内容调用 `GET /api/content`，参数是 `pageNum=1`、`pageSize=6`，不传 `categoryId`。点击一级分类进入分类页，点击子分类进入内容列表，点击最新内容进入详情，编号都来自接口返回值。

分类页同时请求分类详情和分类树。详情用来确认分类存在并显示名称，树用来取出子分类。编号缺失、非法、不存在、没有子分类都有对应状态。

首页和分类页支持下拉刷新。返回页面时不会在 `onShow` 里重复请求。

## 2. 新增文件

- `miniprogram/components/category-card/`
- `miniprogram/components/content-card/`
- `miniprogram/utils/category-tree.ts`
- `miniprogram/utils/content-view.ts`
- `tests/sprint-03b.test.mjs`

## 3. 修改文件

- `miniprogram/pages/index/`
- `miniprogram/pages/category/`
- `miniprogram/app.wxss`
- `miniprogram/utils/error.ts`
- `package.json`

后端 Java、SQL 和测试没有改动。

## 4. API 调用

| 页面 | 接口 |
| --- | --- |
| 首页分类 | `GET /api/categories/tree` |
| 首页最新内容 | `GET /api/content?pageNum=1&pageSize=6` |
| 分类页 | `GET /api/categories/{id}` 和 `GET /api/categories/tree` |

内容列表接口没有排序参数。现有默认顺序是分类排序、内容排序，然后才是 `publish_time DESC`，不是全站按发布时间倒序。这次按现有接口使用，没有为首页改后端。

## 5. 页面截图/运行结果

本机没有微信开发者工具，无法截图，也没有对着运行中的后端做实机请求。页面结构、跳转和状态分支由类型检查和 9 个单元测试覆盖。

## 6. TypeScript 检查

`tsc --noEmit` 通过，0 个错误。

## 7. Lint

项目没有 ESLint，也没有 `.cursor/rules`。没有新增一套 lint 工具。

## 8. Build

`npm run check` 通过：四个页面文件齐全，`wx.request` 仍只在请求封装里，接口地址仍只在 `config/env.ts`。没有微信开发者工具命令行，不能做模拟器编译。

## 9. 测试

`npm run test:mp`：9 个测试，0 失败。覆盖一级分类排序、分类接口失败、内容接口失败、最新内容为空、分类和内容跳转、分类编号缺失、非法、不存在，以及没有子分类。

## 10. Git Diff

当前目录不是 Git 仓库，`git diff` 无法执行。没有初始化仓库，也没有提交。

## 11. 风险

- 首页“最新内容”目前不是全站按发布时间倒序。
- 生产接口地址仍是占位值 `https://api.example.com`。
- 封面为空时使用统一占位，不依赖写死的图片地址。

## 12. 后续建议

03-C 再做内容列表的分页和正式列表界面。在此之前先验收 03-B。
