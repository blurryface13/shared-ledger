# 导出链路只读巡检

执行：`python3 scripts/export_health.py`。仅适用于当前 Docker 本地部署；不消耗消息，不确认/删除消息，不修改数据库或队列。

报告包含 Outbox DEAD、PENDING 数量及最老年龄、业务 FAILED、无 Outbox 的历史 PENDING、业务未完成任务最老年龄、两条队列的消息数及消费者数。默认阈值为等待 300 秒、业务队列 100 条；可通过 `--max-age-seconds` 和 `--max-backlog` 调整。这些是初始运维阈值，不是经过测量的 SLO。

退出码：0 表示本次采集未触发规则；1 表示有待处理问题；2 表示采集失败、超时、依赖不可达或目标队列缺失。2 必须按未知处理，不能显示健康。参数错误同样返回 2。数据库与 RabbitMQ 分别采集，不是同一原子快照。

每个外部命令最多等待 15 秒，报告只包含异常类型，不打印原始 stderr 或命令参数。脚本在 MySQL 容器内读取 MYSQL_MONITOR_USER / MYSQL_MONITOR_PASSWORD；本地未设置时沿用容器 root 凭据。生产接入必须使用只读监测用户，不能复制此本地 root 兜底策略。密码不放进报告或命令行参数值。

## 处置

- OUTBOX_DEAD / EXPORT_FAILED：查任务与原因，修复后通过授权重试接口恢复。
- LEGACY_PENDING_WITHOUT_OUTBOX：历史发送窗口可能留下未投递任务，需要核查；脚本不自动补消息。
- OUTBOX_PENDING_TOO_OLD / EXPORT_UNFINISHED_TOO_OLD：检查 relay、数据库锁等待、broker、消费者及业务处理耗时。
- BUSINESS_QUEUE_BACKLOG / BUSINESS_QUEUE_NO_CONSUMER：检查消费进程、连接、失败重试和资源瓶颈。
- FAILURE_QUEUE_NONEMPTY：按 [失败消息恢复说明](export-failure-retention.md) 逐条排查，不自动清空。
- PROBE_FAILED：先检查探测依赖；故障类型与执行日志关联，勿将 unknown 当作正常。

FAILED 统计会包含尚未处理的历史失败；任务重试时可能短暂保持 FAILED。保留消息也可能重复，因此数量不等于失败任务数。没有告警去重、抑制、关闭事件或持久审计。

## 验证与后续

6 项自动测试覆盖健康、8 类规则、阈值边界、未知状态、退出码与输出脱敏；统一回归入口已加入该测试。真实本地只读检查健康；使用不存在的容器验证探测失败返回 unknown/2，见 [验收记录](export-health-live-result.json)。没有破坏正在运行的依赖。

本轮是可供调度器或告警平台调用的 CLI，不是已部署的定时告警；尚无通知渠道。当前聚合 SQL 可能扫描历史任务，调用频率应低，数据增长后需 EXPLAIN、状态索引与指标采集优化。下一步再做周期采集、告警生命周期及失败通道背压。

## 告警生命周期（E12）

可选参数 `--state-file /absolute/path/state.json` 启用本地状态。首次发现问题输出 opened，持续同一问题不重复输出；可靠采集确认问题消失才输出 resolved。unknown 保留既有业务故障并单独打开 PROBE_FAILED，后续恢复采集只关闭已消失的问题。数值变化仍更新 active.detail，但不逐次发送变更事件。

状态包含监控目标标识，不同数据库/队列不能共用同一文件，否则返回 unknown/2；阈值不属于目标标识，调整阈值可能使事件恢复。文件权限 0600；同目录临时文件写入并 fsync 后原子替换，侧文件锁避免并发写覆盖。锁忙、文件损坏或写入失败不假装成功，不覆盖原状态。当前适用于 macOS/Linux，不支持 Windows 的 fcntl。

事件输出不等于通知投递。lastEvents 只保存最近一次转换，不是持久事件日志；更新状态后若下游未接收 stdout，可能丢失通知，未来通知接入需要单独的事件 Outbox/确认机制。目录未 fsync，因此不宣称断电持久性。仅串行运行巡检；状态写锁不串行化前面的采集阶段。状态丢失后将重新打开当前故障，生产状态不能放在临时目录。

本轮 7 项生命周期测试与既有 6 项巡检测试，共 13 项通过。连续两次真实健康探测均无事件，见 [E12 验收](export-alert-state-result.json)。没有启动后台进程、定时任务或外部通知。
