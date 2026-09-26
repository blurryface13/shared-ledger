# 地图全知视图：实现与验收边界

当前连接 Java 后端的 Web 行程页提供“打开地图”：全程/按天查看地点，地图与列表双向选中，按选中点查 3 公里内景点、住宿、餐饮，搜索地点并加入或关联已有安排，显示当天的真实道路步行路线，以及预报窗口内的天气。手机使用地图加可展开的地点面板。用户主动选择地点；不申请设备定位、不记录行走轨迹。仅当保存表单时才修改行程。

## 数据与坐标

- `MapGateway` 是领域端口；`OsmGateway` 负责地点与道路数据。后端先按行程权限校验，再查询外部服务。
- 地点搜索、城市中心和周边使用 OpenStreetMap Nominatim；步行路线使用 OSRM 的 walking 数据集；地图瓦片使用 OpenStreetMap；天气直接请求 Open-Meteo。前端地图采用 Leaflet 1.9.4，许可证在 `fronted/travel-prototype/vendor/leaflet/LICENSE`。
- 新地点以 WGS84 保存，路线响应也明确标注 WGS84。旧高德地点没有坐标系字段时按 GCJ-02 转为 WGS84 后上图/规划路线，不原地修改旧记录。
- 附近距离是球面直线距离；路线距离、时间取 OSRM 道路结果，不把点间直线画成道路。OSRM 会将 POI 中心吸附到路网：任一端点偏离步行路网超过 250 米时拒绝展示，以免把湖面中心到岛上步道误报为可步行路线，提示改选具体入口或码头。当天不足两个有效地点时提示补足。路线结果携带行程版本，前端拒绝使用过期版本。

## 公共服务约束

- [Nominatim Usage Policy](https://operations.osmfoundation.org/policies/nominatim/) 限制公开实例每应用最多每秒一次请求，要求可识别的 User-Agent 与缓存，也禁止把它当作前端自动补全。当前由用户显式点击触发；Java 串行限速至不快于 1.2 秒/次，地点结果缓存 24 小时。部署时通过 `TRIP_LEDGER_OSM_USER_AGENT` 配置真实联系信息；多人/高流量场景改为自托管或商业地理编码服务。
- [OpenStreetMap tile policy](https://operations.osmfoundation.org/policies/tiles/) 要求可见署名、正常缓存和合理流量。地图保留 OSM/Leaflet 署名；不做批量瓦片预取。公开瓦片服务没有生产可用性保证。
- [Open-Meteo usage/pricing](https://open-meteo.com/en/pricing) 的免 Key 能力适用于其非商业档位；商业部署需核对授权。界面只显示其 16 天预报内有数据的日期。
- [OSRM](https://github.com/Project-OSRM/osrm-backend) 路线与公共服务可能超时或不可达；路线查询是显式操作，失败显示错误，不伪造路线。为提高可用性，生产环境应选有 SLA 的地图/路线服务。
- 地点、路线、瓦片统一使用 OSM 系生态，不把原高德 POI/路线叠到 OSM 底图。过往设计文档的高德方案和“路线仅示意”描述属于历史阶段，以本文件及现行代码为准。

## 还需收尾

1. 将 `app.js` 中旧本地原型逻辑和静态文案从连接版入口拆离，消除两套状态实现；当前连接版已经使用 `connected.js` 数据流。
2. 原生小程序地图层与触摸面板需要真机迁移/验收；当前已验证的是浏览器手机视口。
3. 如果面向真实公众部署，替换公共地理服务为有容量保障的实例，并监测超时、命中率及费用。
4. OAuth、Passkey、OTP 可以参考 [TREK](https://github.com/liketrek/TREK) 的产品能力，但当前登录架构仍是项目自己的微信/本地测试登录，新增认证需要单独的账户绑定与安全设计。TREK 为 AGPL-3.0 许可，此处只参考交互与架构，不复制代码。

参考交互：登录后的 [圆周旅迹](https://www.pitravel.cn/) 用地图点与地点卡片、日程顺序相互定位；这里保留本项目的行程、账本和权限模型，并采用自己的视觉布局。
