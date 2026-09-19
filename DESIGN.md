# Shared Ledger visual direction

用户已确认：2026-09-19 新参考图优先。白天在酒店或街边单手查看行程，浅色主题。

## Palette
Restrained：暖灰纸底 oklch(0.97 0.006 85)，炭灰正文 oklch(0.27 0.008 70)，次要文字 oklch(0.48 0.01 70)。活动用低彩度雾蓝、灰粉、鼠尾草满背景；主要按钮深炭灰。暖黄仅用于汤圆碗与小提示。

## Typography
系统无衬线用于中文与控件。英文旅行副标题可用 Georgia 衬线作为参考图的旅行笔记气质。标题 28–36px，正文 15–16px，辅助信息不小于 12px。数字等宽。

## Layout and components
桌面：窄导航、主时间轴、右侧上下文面板。手机：底部导航、单列主内容、按需导出面板。卡片只代表活动或具体资产，不嵌套卡片。圆角 16–22px。交通行为独立无容器行，空闲时段细斜线背景。

## Export
暖灰粉底的导出舞台，主纸张旋转约 -7°、后页轻微相反倾斜；正文为真实数据，不是截图。阅读模式恢复正视，打印完全去除倾斜和 UI，只输出排版后的文档。

## Motion
150–250ms opacity/transform 过渡；主动路线预览另行提供暂停。尊重 prefers-reduced-motion。无持续漂浮、弹跳或无意义加载编舞。

## Assets
临时汤圆头像用原创矢量草图，等待正式小炫汤圆素材替换。照片由用户上传；不冒充真实相册。地图原型只显示路线示意并标记，真实地图将在 S2 接入。

## 2026-09-19 refinement
用户提供两张角色图：第一张为账本图标，第二张为规划助手头像。界面仅保留功能名称，不显示角色中文名、产品中文名或宣传 slogan。导出纸张加入透视、厚度、多层投影和悬浮层叠。

截图字体辨识为视觉推测：中文接近 PingFang SC Regular / Semibold，系统无衬线数字接近 SF Pro；卡片时间与英文地点名接近 New York / Times 风格。不能仅凭截图确认字体文件。实现以系统无衬线和 ui-serif 为主，Times New Roman 为衬线回退，不下载或分发 Apple 字体。

## Navigation and contextual actions (2026-09-19)

Use Itinerary / Ledger / Gallery for the three main labels. Preserve one responsive design. The left character from the new two-character reference represents Itinerary; the right represents Gallery. Ledger retains its existing image. Ledger uses its own bill-summary paper preview and 帮我算账; Gallery omits itinerary export and assistant controls. Group ledger operations into 明细 / 统计 / 成员 / 审批 / 结算 / 设置 to keep the first screen quiet.

## 入口、文案与字体更新

Trip / Ledger 先进入“已有、新建、历史”列表，再进入详情；历史以手动归档为准，可查看记录并恢复。保持手机底部三入口。创建与关联编辑采用内联表单。

文案用“西湖、午餐、酒店入住”等具体名称，去掉抒情句。中文使用 PingFang SC / 系统黑体，时间、金额、日期统一无衬线等宽数字；英文地名降为辅助信息，不再用衬线。日期牌保留轻微倾斜，增加纸张厚度和投影，点击展开日期与绑定设置，hover/focus 扶正，按压回落，减少动态设置下不做过渡。

## 2026-09-20 手机优先收敛

以用户最新日程 App 参考为准：浅暖灰背景、近白实体卡片、系统字体、紧凑日期切换、左侧时间列。所有宽度统一为手机单列，桌面最大 480px 居中预览。底部三入口保留角色图和英文标签，采用悬浮胶囊，右侧独立新增按钮随行程/账本上下文变化。保留倾斜纸张导出。此轮仍为连接 Java 后端的 Web，小程序原生迁移另行实施。

## 2026-09-20 单日面板

小程序方向不变，借鉴 OffWeGo 单日列表：深色选中日期、整块白色圆角面板、图标与简洁条目、底部圆形新增。点击行程打开底部编辑面板，选择日期实现跨天移动。右侧手柄支持长按拖动和点击后上移/下移，时间改动必须预览确认，通过原有版本接口保存。手机浏览器验证与原生小程序真机验收是两个阶段。
