# Sprint 03-D 完成报告

内容详情已按 `contentType` 分流：文章走文章接口，视频走视频接口。没有进入 Sprint 04，也没有改后端。

## 1. 完成内容

详情页接收 `id`，先请求内容详情，再按类型请求文章或视频。

- `ARTICLE` 展示标题、作者、来源、发布时间、封面和正文。
- `VIDEO` 展示标题、封面、来源、简介、二维码、时长和发布时间。
- `QUESTION`、`TOPIC`、`WEEKLY`、`DOCUMENT` 显示“该内容类型暂不支持”。
- 参数缺失、参数非法、内容不存在、文章不存在、视频不存在和加载失败都有对应提示。
- 页面提供“返回”。列表页没有 `onShow` 重载，从详情返回后保留列表状态。

## 2. 修改文件

- `miniprogram/pages/content-detail/content-detail.ts`
- `miniprogram/pages/content-detail/content-detail.wxml`
- `miniprogram/pages/content-detail/content-detail.wxss`
- `miniprogram/pages/content-detail/content-detail.json`
- `miniprogram/services/content.ts`
- `package.json`

`getArticleDetail` 和 `getVideoDetail` 已从 `content.ts` 拆出，避免三个接口挤在同一个 service 里。已有 `ArticleDetailVO`、`VideoDetailVO` 继续复用，没有再定义一份。

## 3. 新增文件

- `miniprogram/services/article.ts`
- `miniprogram/services/video.ts`
- `miniprogram/utils/content-detail.ts`
- `tests/sprint-03d.test.mjs`

## 4. Content API

`contentService.getContentDetail(id)` → `GET /api/content/{id}`

实际字段与 `ContentDetailVO` 一致：`id`、`title`、`contentType`、`categoryId`、`coverUrl`、`summary`、`status`、`sort`、`viewCount`、`favoriteCount`、`publishTime`。编号是字符串。

`id` 为空显示“内容参数缺失”，不是数字显示“内容参数无效”，这两种情况不发请求。404 显示“内容不存在”。

## 5. Article API

仅当 `contentType === ARTICLE` 时调用 `articleService.getArticleDetail(contentId)` → `GET /api/articles/{contentId}`。

字段与 `ArticleDetailVO` 一致：`contentId`、`title`、`categoryId`、`coverUrl`、`summary`、`status`、`sort`、`publishTime`、`body`、`author`、`source`。

文章接口 404 显示“文章内容不存在”，不会再去请求视频。

## 6. Video API

仅当 `contentType === VIDEO` 时调用 `videoService.getVideoDetail(contentId)` → `GET /api/videos/{contentId}`。

字段与 `VideoDetailVO` 一致：`contentId`、`title`、`categoryId`、`coverUrl`、`summary`、`status`、`sort`、`publishTime`、`sourceType`、`videoUrl`、`qrCodeUrl`、`duration`。

视频接口 404 显示“视频内容不存在”，不会再去请求文章。

后端来源只有 `WECHAT_CHANNEL`、`TENCENT_VIDEO`。`duration` 单位是秒。公开视频详情里的封面、简介来自内容表。

## 7. 文章展示方式

正文使用 `rich-text`，没有把 HTML 放进 `{{body}}`。展示前会去掉 `script`、`iframe` 等标签和事件属性，并给图片加上 `max-width:100%`。纯文本会转义后再交给 `rich-text`。日期沿用已有的 `formatPublishDate`，格式为 `YYYY-MM-DD`。封面为空时显示“暂无封面”，没有另写图片地址。

## 8. 视频展示方式

没有使用 `video` 组件，也没有调用 `wx.createVideoContext`。`videoUrl` 在后端只是最长 512 的外部地址，小程序不能随意打开未配置业务域名的网页。页面因此提示扫码观看，不把这个地址当成可播放源。

来源显示为“微信视频号”或“腾讯视频”。其他值显示“暂不支持的播放来源”。封面优先用视频详情的 `coverUrl`，为空再用内容详情的 `coverUrl`，仍为空则显示“暂无封面”。时长 `630` 显示为 `10:30`，满一小时显示 `HH:MM:SS`。

## 9. 二维码处理

`qrCodeUrl` 有值时展示图片，文案是“扫码观看视频”，点击后调用 `wx.previewImage` 预览。没有调用保存到相册。`qrCodeUrl` 为空时不渲染图片，文案是“请扫码观看视频”。

## 10. 状态处理

- `loading`：加载中
- `success`：文章或视频
- `error`：参数错误或请求失败。参数错误只提供返回；请求失败提供“重新加载”
- `unsupported`：该内容类型暂不支持

网络失败仍使用已有文案“网络异常，请稍后重试”。服务端 500 和内部异常显示“内容加载失败，请稍后重试”。

## 11. TypeScript

`tsc --noEmit` 通过，0 error。

## 12. Test

`npm run test:mp`：42 passed，0 failed。其中 Sprint 03-D 19 项，覆盖参数、内容 404、加载失败、文章成功与失败、HTML 正文、视频成功与失败、两种来源、二维码有无、时长、不支持的类型、列表跳转和返回列表。

文章只请求 content 和 article。视频只请求 content 和 video。

## 13. Check / Build

`npm run check`：`miniprogram structure ok, pages=4`。

项目没有 `npm run build`，也没有 ESLint。`wx.request` 仍只在 `miniprogram/services/request.ts`。

## 14. Git 状态

Git diff unavailable

当前目录不是 Git 仓库。没有初始化仓库，也没有提交。

## 15. 后端是否修改

没有修改。Sprint 02 后端变更数为 0。

## 16. 风险

- 后端保存文章正文时没有 HTML 清洗。前端只做了标签和事件属性剔除，不能替代完整的 HTML 安全处理。
- `rich-text` 内部图片加载失败时，页面无法单独替换成占位图。
- 视频不能在小程序内直接播放。没有二维码时，用户只能看到“请扫码观看视频”。
- 未对运行中的后端做联调。

## 17. 待微信开发者工具验证事项

当前环境没有微信开发者工具，不能声称模拟器验证通过。

待验证：

- 文章 `rich-text` 的正文、长图和图片宽度
- 二维码预览
- 从内容列表进入详情后再返回，列表页码和已加载数据是否保持
- 文章、视频、不支持类型三种页面的实际排版

等待 Sprint 03-D 验收。
