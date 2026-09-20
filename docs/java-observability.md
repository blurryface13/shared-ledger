# Java 可观测性接入

引入 Spring Boot Actuator、Micrometer 与 Prometheus registry。由 Spring Boot 管理兼容版本，Java 业务服务直接暴露 JVM、进程、HTTP、连接池及自定义导出指标。

## 访问边界

默认管理端口 9091，仅监听 127.0.0.1；通过 TRIP_LEDGER_MANAGEMENT_PORT 可调整，必须保持独立于业务端口。仅开放 health/prometheus，health 不返回详情。安全过滤器只对白名单两个端点检查实际本地接收端口，其余 /actuator/** 一律拒绝；对允许路径检查实际本地接收端口，不能用转发头冒充管理端口。测试 profile 关闭管理监听。

本机采集地址为 http://127.0.0.1:9091/actuator/prometheus。不要让前端代理或公网反向代理转发该端口。这里使用本机网络隔离，不是运维用户认证；同机进程可读取指标。多主机或容器采集需重新设计专用网络与认证，不能简单绑定公网。不要将管理端口与业务端口配置为同一个值。

## 导出指标

- tripledger_export_pending：等待发布数。
- tripledger_export_dead：发布耗尽数。
- tripledger_export_failed：业务失败数，可能包含历史失败。
- tripledger_export_oldest_pending_seconds：最老待发布记录年龄。
- tripledger_export_collection_success：数据库采集成功为 1，失败为 0；失败时其他导出 gauge 为 -1，而非健康零值。

所有导出 gauge 共用最多 5 秒缓存的数据库采样，单 JVM 同步刷新，不包含用户/任务 ID 标签。只在抓取时查询；此版仍在抓取线程执行查询，慢数据库可能影响抓取耗时。当前没有采集专用连接池或查询硬截止，不宣称已解决抓取隔离；数据增长后应评估聚合 SQL、索引与后台采样。

## 尚未完成

Prometheus server、Grafana、Alertmanager 未部署，没有通知渠道。提供可抓取指标不等于已经保留历史、建立告警或得到性能结论。RabbitMQ 队列状态仍通过已有只读巡检检查，自定义 Java 指标只覆盖数据库状态。后续应补采集服务、规则测试、低基数 HTTP 路由验证、数据库异常下抓取耗时测量。
