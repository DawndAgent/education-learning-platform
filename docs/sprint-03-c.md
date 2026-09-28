# Sprint 03-C 完成报告

分类页进入内容列表后，可以按分类浏览文章和视频，并支持分页加载。没有进入 03-D，也没有改后端。

## 1. 完成内容

分类页进入内容列表后，可以按 `categoryId` 浏览文章和视频，并支持分页。

- 首次只请求第 1 页。
- 触底追加下一页，重复触底不会再次发同一页请求。
- 下拉刷新从第 1 页重新加载，并替换列表。
- 分类参数缺失、非法、分类不存在、空列表、首次失败、加载更多失败都有对应提示。
- 点击卡片跳到 `pages/content-detail/content-detail?id={接口返回的 contentId}`。
- 后端排序未改，仍是分类 sort、内容 sort、`publish_time DESC`。

## 2. 修改文件

- `miniprogram/pages/content-list/content-list.ts`
- `miniprogram/pages/content-list/content-list.wxml`
- `miniprogram/pages/content-list/content-list.json`
- `miniprogram/pages/content-list/content-list.wxss`
- `miniprogram/components/content-card/content-card.ts`
- `miniprogram/components/content-card/content-card.wxml`
- `miniprogram/components/content-card/content-card.wxss`
- `miniprogram/components/error/error.ts`
- `miniprogram/components/error/error.wxml`
- `package.json`

## 3. 新增文件

- `miniprogram/utils/content-list.ts`
- `tests/sprint-03c.test.mjs`

## 4. API 实际调用方式

页面只调用 service：

- `contentService.getContentList()` → `GET /api/content`
- `categoryService.getCategoryDetail()` → `GET /api/categories/{id}`

查询参数为 `categoryId`、`pageNum`、`pageSize=10`。可选的 `contentType` 仍会带上。页面内没有 `wx.request`，也没有拼接 API 地址。

分类名称来自分类详情。内容列表对不存在的分类返回空页，所以“分类不存在”以分类详情的 404 为准。

## 5. 分页结构

与后端 `PageResult` 一致：

- `pageNum`
- `pageSize`
- `total`
- `records`

没有 `hasNext` 或 `totalPages`。是否还有下一页：已加载条数小于 `total`。若追加后因 `contentId` 去重没有新数据，则停止继续加载。

## 6. 首次加载

`onLoad` 读取并校验 `categoryId`，通过后初始化分页，同时请求分类详情和第一页内容。`onShow` 不会重新请求。

## 7. 上拉加载更多

`onReachBottom` 在 `hasMore` 时请求 `pageNum + 1`，结果追加到已有列表。底部在请求中显示“正在加载...”，全部加载完显示“没有更多内容”。空闲时不显示额外文案。

## 8. 下拉刷新

`onPullDownRefresh` 重新请求第 1 页并替换列表。成功或失败都会调用 `wx.stopPullDownRefresh()`。

## 9. 并发控制

- `loadingMore === true` 时，再次触底直接返回。
- `refreshing === true` 时，不发起加载更多。
- 首次加载和刷新会增加 generation。过期的加载更多结果会被忽略，避免覆盖新数据。

## 10. 空状态

分类存在且第一页 `records` 为空时，显示“暂无内容”，并保留“重新加载”和“返回上一页”。

## 11. 错误状态

- 参数缺失：“分类参数缺失”，只提供返回。
- 参数非法，例如 `abc`：“分类参数无效”，只提供返回。
- 分类详情 404：“分类不存在”。
- 首次加载失败：“内容加载失败，请稍后重试”，并提供“重新加载”。网络失败仍使用已有文案“网络异常，请稍后重试”。
- 加载更多失败保留已有列表，提示“加载更多失败，请重试”，再次触底可以重试。

## 12. TypeScript

`tsc --noEmit` 通过，0 error。

## 13. Test

`npm run test:mp`：23 passed，0 failed。其中 Sprint 03-C 14 项，覆盖参数、首页加载、空列表、失败、第 2/3 页追加、`hasMore`、重复触底、刷新替换、过期结果忽略、加载更多失败保留数据、详情跳转，以及分类不存在。

## 14. Build / Check

`npm run check`：`miniprogram structure ok, pages=4`。

项目没有 ESLint 脚本，未另加 lint。

当前环境没有微信开发者工具，列表页没有在模拟器里点选验证。分页行为由上述测试覆盖。

## 15. Git 状态

Git diff unavailable

当前目录不是 Git 仓库。没有初始化仓库，也没有提交。

## 16. 后端是否修改

没有修改。Sprint 02 后端变更数为 0。

## 17. 风险

- 列表顺序沿用后端默认排序，不是按发布时间全局倒序。
- 分页结束依赖 `total`。若后端 `total` 偏大且后续页重复同一批 `contentId`，去重后会停止继续加载。
- 未对真实接口做联调。分类不存在时，内容接口本身返回空列表，页面依赖分类详情的 404。

## 18. 下一步建议

等待 Sprint 03-C 验收。下一阶段是 Sprint 03-D：文章详情和视频详情。本阶段停在这里。
