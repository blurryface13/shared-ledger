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
