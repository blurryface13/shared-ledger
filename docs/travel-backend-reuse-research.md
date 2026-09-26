# Spring Boot 旅行规划复用调研

调查日期：2026-09-19。近两年窗口：2024-09-19 至调查日。搜索 GitHub 相关项目，使用 GitHub REST API 实时核验星数、创建时间、pushed_at、许可证，并抽查关键源码。pushed_at 仅表示仓库最后推送，不等同于功能发布日期。未运行候选项目或其测试，未导入第三方代码。星数为调查快照，不代表质量保证。

## 候选

| 仓库 | Stars | 创建 / 最后推送 | 许可证 | 本项目用途 |
|---|---:|---|---|---|
| [TripFlow](https://github.com/codeurjc-students/2025-TripFlow) | 1 | 2025-07-05 / 2026-08-27 | Apache-2.0 | 优先评估行程 CRUD、权限、地图端口、路线 DTO、缓存和测试场景 |
| [TripStar-Java](https://github.com/LeeFly-cn/TripStar-Java) | 58 | 2026-07-06 / 2026-07-20 | GPL-2.0 | 国内 POI、天气、规划任务阶段和结构化输出参考，代码迁移先评估许可证兼容性 |
| [AiClient](https://github.com/Cooosin/AiClient) | 148 | 2025-06-05 / 2025-06-17 | MIT | 小体量 MCP/工具调用学习与局部适配，不适合当业务底座 |
| [AI-Tourism-Assistant-Agent](https://github.com/kmoonn/AI-Tourism-Assistant-Agent) | 16 | 2025-06-19 / 2026-04-20 | MIT | 天气 MCP 和轻量 Agent 候选，尚未深入实现核验 |
| [ai-tourism](https://github.com/1937983507/ai-tourism) | 129 | 2025-09-20 / 2026-03-09 | API 未识别许可证 | 场景贴近，但未见明确 LICENSE，暂不纳入代码搬迁名单 |
| [embabel/tripper](https://github.com/embabel/tripper) | 152 | 2025-06-10 / 2026-03-14 | Apache-2.0 | Kotlin + Spring Boot/Embabel，Agent 设计参考，增加新语言与框架不符合当前主线 |
| [tourism-agent](https://github.com/smyjl15/tourism-agent) | 0 | 2026-06-27 / 2026-07-29 | MIT | 约束规划评测参考；README 明确包含演示数据，不能当真实路线服务 |
| [Youji](https://github.com/ouli-1242/Youji-Travel-Sharing-Platform) | 1 | 2026-07-17 / 2026-07-17 | API 未识别许可证 | Spring Boot / MyBatis-Plus / uni-app 很贴近，但暂无明确复用授权依据 |

本轮未找到同时满足高星（例如千星）、近期活跃、成熟 Spring Boot 旅行规划业务且可以低成本整体搬迁的项目。百星候选多偏 Agent 示例。优先级按可复用业务代码判断，而非星数排名。

## 已检查的源码证据

### TripFlow：优先作为传统后端模块来源

- [backend/pom.xml](https://github.com/codeurjc-students/2025-TripFlow/blob/main/backend/pom.xml)：Java 21、Spring Boot 3.5.9。API 使用 JPA/PostgreSQL；项目含 Kafka 与多个服务。迁入现有 MyBatis-Plus/MySQL/RabbitMQ 模块化单体需要改造，不能直接替换整个 backend。
- [OpenRouteServiceRoutingProvider](https://github.com/codeurjc-students/2025-TripFlow/blob/main/backend/api-service/src/main/java/com/tripflow/service/map/provider/routing/OpenRouteServiceRoutingProvider.java)：真实调用 directions，支持驾车/步行/骑行，解析 GeoJSON 坐标、分段距离和时长，构造缓存键，有超时错误映射。
- [ItineraryService](https://github.com/codeurjc-students/2025-TripFlow/blob/main/backend/api-service/src/main/java/com/tripflow/service/itinerary/ItineraryService.java)：读写删权限检查、行程/日程服务与 DTO 映射，update 有事务。可用作行为对照，领域模型应按本仓库重建。
- 递归树发现 47 个 Java 测试文件（包括测试辅助类，并非 47 条测试）；涵盖 MapEndpointsTest、DirectionsCacheKeyBuilderTest、ItineraryPermissionServiceTest 等。仅确认存在，未声称测试通过或覆盖率达标。
- 待进一步验证：真实供应商密钥下的路线质量、API 版本兼容、缓存授权、失败数据处理。解析代码对缺少 distance/duration 默认取 0，我们应改为显式缺失/失败，不显示成免费或零耗时。

### TripStar-Java：高德与 Agent 场景更贴近

- README 技术栈 Java 21 / Boot 4.0.7 / Spring AI 2.0.0-M1 / Spring AI Alibaba 2.0.0-M1.1。里程碑依赖和大量 Agent 编排不宜整体引入。
- [AmapMapContextService](https://github.com/LeeFly-cn/TripStar-Java/blob/main/modules/map/src/main/java/com/zkry/map/service/AmapMapContextService.java)：核验了地理编码、POI、天气、4 秒连接超时、8 秒请求超时及限速重试。该类未发现道路 directions 实现，不能仅凭 README 的地图展示承诺 Java 后端路线模块已齐全。
- [TripTaskService](https://github.com/LeeFly-cn/TripStar-Java/blob/main/modules/trip/src/main/java/com/zkry/trip/service/TripTaskService.java)：任务放 ConcurrentHashMap，使用 newCachedThreadPool，返回任务 ID 后异步运行、推送进度。不能原样用于需要重启恢复、容量限制、多实例的上线任务系统。
- GPL-2.0 已确认；直接复制代码前应核对目标仓库许可证与分发要求。当前仅研究职责拆分，不导入代码。

### AiClient：小而容易看懂，不能替代项目底座

- 树中仅发现 8 个 Java 主源码文件。
- [TravelService](https://github.com/Cooosin/AiClient/blob/master/src/main/java/com/coosin/aiclient/service/TravelService.java) 输出目标为 travel.html；更接近工具调用生成攻略的演示，而非可编辑、可版本化的行程 CRUD。
- [pom.xml](https://github.com/Cooosin/AiClient/blob/master/pom.xml) Boot 3.4.5 / Java 21，同时单独固定 WebFlux starter 3.2.2，依赖版本不宜照搬。

## 迁移建议

保持现有共享账本、身份、结算、导出和 DDD 分层。采用“模块移植 + 边界适配”，不 fork 后用新项目替换现有后端。

1. **先补 Trip 核心**：Trip、TripDay、Activity，独立创建、归档、恢复、顺序调整、版本字段与可选账本关联。参考 TripFlow 的请求/响应与权限测试行为；持久化采用现有技术栈。
2. **接真实路线**：领域定义 PlaceSearchPort、RouteQueryPort；借鉴 TripFlow provider/DTO/cache 的拆分，基础设施实现高德适配器。返回坐标系、道路几何、距离、耗时、供应商与查询时间。无需定位。
3. **最后加 Agent**：输出结构化 PlanDraft，先展示差异、校验后采纳。参考 MIT 项目的工具调用骨架，工具只调现有 POI/路线应用服务；不一开始引入小红书抓取、多 Agent、Milvus 或第二套登录体系。
4. **上线补强自己掌握**：MySQL 任务持久化、RabbitMQ 投递/幂等消费、事务 outbox、任务版本检查、超时与有界重试、Redis 缓存、成员权限、审计。它们也是 Java 面试与项目讲解的核心。

可直接节省较多查接口和摸索流程的工作；暂不能承诺“复制百分之多少代码”。需要先跑通候选最小模块、核对授权并验证 Boot 版本适配，才能估算迁移量。复制 Apache/MIT 文件也需保留适用版权、许可证与修改说明。

未明确许可证的公共仓库不等同于获准复制分发，依据：[GitHub licensing documentation](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository)。

## 用户确认后的优先级调整

传统 Java 后端求职为主，技术栈一致优先。TripFlow 降为“模块和测试行为参考”，不迁入 JPA/PostgreSQL/Kafka 服务架构；TripStar 栈接近但授权和上线设计仍需独立评估。先复用本仓库已有业务，再按需借用独立适配代码。Agent 仅做意图识别和结构化草案生成，规则与执行归 Java。实施与证据要求见 [后端实施计划](backend-implementation-plan.md)。
