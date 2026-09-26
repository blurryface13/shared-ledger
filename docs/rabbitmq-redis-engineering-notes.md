# RabbitMQ 与 Redis 工程接入说明

这份文档按代码阅读顺序解释本次改造。目标不是把项目变复杂，而是让它具备能讲清楚的工程思想：耗时任务异步化、并发写保护、重复提交拦截。

## 1. 新增代码在项目里的位置

### RabbitMQ 相关

- `backend/trip-ledger/pom.xml`
  - 新增 `spring-boot-starter-amqp`，让 Spring Boot 可以连接 RabbitMQ、声明队列、发送消息、监听消息。
- `backend/trip-ledger/src/main/resources/application.properties`
  - 新增 RabbitMQ 连接配置与导出队列配置。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/shared/infrastructure/messaging/RabbitMqConfiguration.java`
  - 声明交换机、队列、绑定关系、JSON 消息转换器。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/shared/infrastructure/messaging/RabbitMqExportProperties.java`
  - 承接 `app.rabbitmq.*` 配置。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/export/infrastructure/messaging/ExportTaskMessage.java`
  - RabbitMQ 里传递的消息体，目前只包含 `exportRecordId`。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/export/infrastructure/messaging/ExportTaskPublisher.java`
  - 生产者，把导出任务投递到 RabbitMQ。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/export/infrastructure/messaging/ExportTaskConsumer.java`
  - 消费者，监听队列并异步处理导出任务。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/export/application/ExportApplicationService.java`
  - 导出业务从同步生成结果，改成创建任务、投递消息、异步更新状态。

### Redis 相关

- `backend/trip-ledger/pom.xml`
  - 新增 `spring-boot-starter-data-redis`。
- `backend/trip-ledger/src/main/resources/application.properties`
  - 新增 Redis 连接配置、锁 key 前缀、幂等 key 前缀。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/shared/infrastructure/redis/RedisConcurrencyProperties.java`
  - 承接 `app.redis.*` 配置。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/shared/infrastructure/redis/RedisDistributedLockService.java`
  - 基于 Redis `SET NX EX` 思想实现轻量分布式锁。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/shared/infrastructure/redis/RedisIdempotencyService.java`
  - 基于 Redis `setIfAbsent` 实现接口幂等保护。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/billing/interfaces/rest/bill/BillController.java`
  - 账单创建、修改、删除、成员结算入口接入 Redis 分布式锁；创建账单入口接入幂等。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/billing/interfaces/rest/request/BillRequestController.java`
  - 账单审批通过/拒绝入口接入 Redis 幂等。

### 数据库与 Docker

- `sql/docker-compose.yml`
  - 新增 Redis 和 RabbitMQ 服务。
- `sql/docker/mysql/initdb/001-init.sql`
  - 新库初始化时，`tb_export_record` 已包含异步任务状态字段。
- `sql/docker/mysql/migrate/20260607_export_async_status.sql`
  - 老库迁移脚本，给 `tb_export_record` 增加 `export_status`、`started_at`、`finished_at`、`error_message`。
- `backend/trip-ledger/src/main/java/com/spvermicelli/tripledger/shared/domain/enums/ExportStatus.java`
  - 导出任务状态枚举：`PENDING`、`RUNNING`、`SUCCESS`、`FAILED`。

## 2. RabbitMQ Part 0：它到底是什么

RabbitMQ 是一个消息队列服务，不是 Java 里的一个普通函数。

原来一个接口的流程是：

```text
前端请求 -> 后端执行完整导出 -> 返回导出结果
```

如果导出很慢，前端会一直等，后端请求线程也会一直被占用。

接入 RabbitMQ 后，流程变成：

```text
前端请求 -> 后端创建导出任务 -> 投递消息到 RabbitMQ -> 立刻返回任务 ID
RabbitMQ -> 后端消费者异步处理 -> 更新任务状态
前端 -> 查询任务状态 -> 看到 SUCCESS 后下载/查看结果
```

它解决的是“把长耗时工作从主请求链路里拿出去”的问题。

## 3. RabbitMQ Part 1：配置队列

文件：`RabbitMqConfiguration.java`

核心代码：

```java
@Bean
public DirectExchange exportExchange(RabbitMqExportProperties properties) {
    return new DirectExchange(properties.getExportExchange(), true, false);
}

@Bean
public Queue exportQueue(RabbitMqExportProperties properties) {
    return new Queue(properties.getExportQueue(), true);
}

@Bean
public Binding exportBinding(Queue exportQueue, DirectExchange exportExchange, RabbitMqExportProperties properties) {
    return BindingBuilder.bind(exportQueue)
        .to(exportExchange)
        .with(properties.getExportRoutingKey());
}
```

这里有三个概念：

- `Exchange`：消息先发到交换机。
- `Routing Key`：消息的路由标识。
- `Queue`：真正排队等待消费的地方。

本项目使用的是 `DirectExchange`，意思是 routing key 精确匹配后，把消息投递到绑定的队列。

## 4. RabbitMQ Part 2：生产者如何发消息

文件：`ExportTaskPublisher.java`

```java
public void publish(Long exportRecordId) {
    rabbitTemplate.convertAndSend(
        properties.getExportExchange(),
        properties.getExportRoutingKey(),
        new ExportTaskMessage(exportRecordId)
    );
}
```

输入：

- `exportRecordId`：数据库里的导出任务 ID。

输出：

- 没有直接业务返回值。
- RabbitMQ 里多了一条消息。

关键点：消息里只放任务 ID，不放完整导出数据。这样消息体很轻，真正的数据仍然由 consumer 从数据库和业务服务里读取。

## 5. RabbitMQ Part 3：导出接口前后区别

文件：`ExportApplicationService.java`

以前导出接口会直接生成内容：

```text
exportPersonalDetail()
  -> 查统计
  -> 查账单
  -> 拼 JSON
  -> 返回 exportContentJson
```

现在变成：

```java
public ExportRecordResult exportPersonalDetail(Long currentUserId, Long bookId) {
    BookMember currentMember = requireActiveMember(bookId, currentUserId);
    return createExportTask(bookId, currentMember.getId(), ExportType.PERSONAL_DETAIL);
}
```

`createExportTask` 做三件事：

```java
ExportRecord savedRecord = exportRecordRepository.save(ExportRecord.builder()
    .bookId(bookId)
    .operatorMemberId(operatorMemberId)
    .exportType(exportType)
    .exportStatus(ExportStatus.PENDING)
    .fileUrl(null)
    .createdAt(LocalDateTime.now())
    .build());
publishExportTaskAfterCommit(savedRecord.getId());
```

这表示：

- 数据库先插入一条 `PENDING` 任务。
- 事务提交后再发 RabbitMQ 消息。
- 接口立即返回 `exportRecordId` 和 `exportStatus=PENDING`。

为什么要 `afterCommit`？

因为如果数据库还没提交，RabbitMQ consumer 可能已经收到消息并查询任务，结果查不到这条记录。`afterCommit` 保证消息发出时，任务记录已经落库。

## 6. RabbitMQ Part 4：消费者如何处理任务

文件：`ExportTaskConsumer.java`

```java
@RabbitListener(queues = "${app.rabbitmq.export-queue}")
public void consume(ExportTaskMessage message) {
    exportApplicationService.processExportTask(message.exportRecordId());
}
```

输入：

- RabbitMQ 消息：`ExportTaskMessage(exportRecordId)`。

输出：

- 没有 HTTP 返回。
- 数据库中的 `tb_export_record` 状态会被更新。

真正处理逻辑在 `processExportTask`：

```java
exportStatus = RUNNING
生成导出快照
exportStatus = SUCCESS
```

如果失败：

```java
exportStatus = FAILED
errorMessage = exception.getMessage()
```

所以前端看到的不是“接口卡住等结果”，而是：

```text
PENDING -> RUNNING -> SUCCESS
```

或者：

```text
PENDING -> RUNNING -> FAILED
```

这就是异步任务状态机。

## 7. RabbitMQ 你作为求职者需要掌握到什么程度

不用把 RabbitMQ 讲成专家，但要能讲清楚：

- 为什么不用同步接口：导出、OCR、复杂结算这类任务可能耗时长。
- 为什么用队列：削峰、解耦、失败重试、异步消费。
- 消息里放什么：通常放任务 ID，不放大对象。
- 数据库状态怎么设计：`PENDING/RUNNING/SUCCESS/FAILED`。
- 失败怎么办：记录 `errorMessage`，必要时重试或人工补偿。
- 为什么要事务提交后发消息：避免 consumer 查不到未提交任务。

可以这样描述：

> 我将导出接口从同步返回结果改造为异步任务模型。接口只负责创建任务记录并投递 RabbitMQ，consumer 异步生成导出快照并更新任务状态，前端通过轮询任务状态感知完成情况，从而降低长耗时任务对主请求线程的阻塞。

## 8. Redis Part 0：登录注册验证里 Redis 可以做什么

当前项目登录核心是 JWT + refresh token：

```text
微信 code 登录
-> 后端校验/创建用户
-> 生成 access token
-> 生成 refresh token
-> refresh token hash 存数据库
-> 前端保存 token
```

当前代码里，登录验证的强一致数据仍然放数据库：

- access token：JWT，本身可被后端解析。
- refresh token：明文给前端，hash 存入 `tb_user_refresh_token`。
- 用户是否注销/可用：后端查用户状态。

Redis 在登录注册场景里常见用途是：

- 缓存短信验证码或图形验证码，key 类似 `login:sms:手机号`。
- 缓存微信登录临时态，key 类似 `wechat:login:code`。
- 缓存用户会话状态，减少每次鉴权查数据库。
- 保存 token 黑名单，比如用户主动退出后，把某个 token 加入 Redis 黑名单直到过期。
- 做登录限流，比如同一手机号 1 分钟只能请求一次验证码。

本次代码没有把 refresh token 从数据库迁移到 Redis，因为 refresh token 属于安全关键数据，数据库更适合作为最终可信存储。文档里把 Redis 登录场景作为 Part 0，是为了让你面试时能讲清楚：Redis 可以参与登录，但不能盲目替代数据库。

可以这样讲：

> 当前项目 refresh token 使用数据库保存 hash，保证会话可追溯和可撤销；Redis 更适合放短期验证码、登录限流、token 黑名单或用户会话缓存，作为加速和防滥用手段，而不是替代数据库里的安全会话记录。

## 9. Redis Part 1：分布式锁解决什么问题

问题场景：

如果将来我们维护了这些聚合数据：

- 账本总支出
- 账本预算进度
- 成员应收金额
- 成员应付金额
- 分类支出汇总

多人同时改账单时可能出现丢更新。

例子：

```text
账本当前总支出 = 100
用户 A 新增账单 20，读到 100，准备写 120
用户 B 新增账单 30，也读到 100，准备写 130
最后数据库可能是 130，而不是 150
```

这就是并发写导致的丢更新。

解法有三类：

- 不存聚合字段：实时计算，必要时 Redis 缓存，避免写冲突。
- 乐观锁：表里加 `version`，更新时校验版本号。
- 分布式锁：按 `bookId` 加锁，让同一账本写操作串行化。

本项目这次使用的是第三种思想：按账本 ID 加 Redis 锁。

## 10. Redis Part 2：分布式锁代码怎么工作

文件：`RedisDistributedLockService.java`

核心获取锁代码：

```java
Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, lockValue, leaseTime);
```

这是 Redis 的 `SET key value NX EX seconds` 思想：

- `key`：锁的名字，比如 `trip-ledger:lock:book:1`。
- `value`：随机 UUID，代表这次请求持有锁。
- `NX`：只有 key 不存在时才设置成功。
- `EX`：设置过期时间，避免服务崩溃后死锁。

输入：

- `bookId`
- 等待时间：代码里是 3 秒。
- 锁租约时间：代码里是 20 秒。
- 真正要执行的业务函数。

输出：

- 如果抢到锁，执行业务函数并返回业务结果。
- 如果 3 秒内抢不到锁，抛出 `CONFLICT`。

释放锁代码：

```java
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
end
return 0
```

为什么释放锁要用 Lua？

因为要保证“判断 value 是不是自己”和“删除 key”这两个动作是原子的。否则可能出现 A 的锁过期后 B 拿到锁，A 又误删了 B 的锁。

## 11. Redis Part 3：分布式锁接在哪里

文件：`BillController.java`

创建共享账单：

```java
redisDistributedLockService.executeWithBookLock(bookId, () ->
    billApplicationService.createSharedExpenseBill(...)
)
```

修改账单、删除账单、结算账单也使用同样方式。

为什么放在 Controller，而不是放在 Service 方法内部？

因为 Service 方法有 `@Transactional`。如果锁在 Service 方法内部释放，可能早于事务真正提交。放在 Controller 外层时，调用 Service 返回前事务已经提交，锁覆盖范围更完整。

这里的工程思想是：

```text
同一账本的写操作串行化
不同账本的写操作并行
```

这比全局锁更合理。

## 12. Redis Part 4：幂等解决什么问题

问题场景：

- 用户连点“新增账单”
- 网络慢，前端重复发送
- 手机端重试
- 审批按钮被点两次

如果没有幂等保护，可能导致：

- 同一笔账单写入两次
- 同一个审批动作执行两次
- 同一条操作日志记录两次

幂等的意思是：同一个业务动作重复执行多次，结果应该和执行一次一样。

## 13. Redis Part 5：幂等代码怎么工作

文件：`RedisIdempotencyService.java`

核心代码：

```java
Boolean created = redisTemplate.opsForValue().setIfAbsent(redisKey, value, ttl);
if (!Boolean.TRUE.equals(created)) {
    throw new BusinessException(ErrorCode.CONFLICT, "请求正在处理或已提交，请勿重复操作");
}
```

输入：

- `businessKey`：业务唯一键。
- `ttl`：幂等窗口时间。
- `action`：真正要执行的业务函数。

输出：

- 第一次请求：设置 Redis key 成功，执行业务。
- 第二次请求：Redis key 已存在，直接拒绝。

本项目两个使用点：

### 新增账单防重复

文件：`BillController.java`

```java
"bill:create-shared:" + currentUserId + ":" + bookId + ":" + title + ":" + amount + ":" + billTime
```

含义：

- 同一用户
- 同一账本
- 同一标题
- 同一金额
- 同一账单时间

30 秒内重复提交，会被认为是重复点击。

### 审批防重复

文件：`BillRequestController.java`

```java
"bill-request:approve:" + bookId + ":" + requestId + ":" + currentUserId
```

含义：

- 同一个用户
- 对同一个申请
- 执行同一个审批动作

5 分钟内重复审批，会被拦截。

## 14. Redis 你作为求职者需要掌握到什么程度

需要能讲清楚这几件事：

- Redis 是内存型 key-value 数据库，适合做缓存、锁、幂等、限流、验证码、短期状态。
- 分布式锁的核心是 `SET key value NX EX seconds`。
- 锁必须有过期时间，避免死锁。
- 释放锁必须校验 value，避免删掉别人的锁。
- 幂等 key 要根据业务构造，不是随便用一个 UUID。
- TTL 要合理：太短拦不住重试，太长影响用户正常操作。
- Redis 不是所有数据的最终真相，关键业务状态仍应落数据库。

可以这样描述：

> 我在账单写入链路中引入 Redis 分布式锁，按账本 ID 对创建、修改、删除、结算操作进行串行化，避免多人同时修改同一账本时出现聚合数据丢更新风险；同时使用 Redis 幂等 key 拦截新增账单和审批接口的重复提交，减少连点和网络重试导致的重复写入。

## 15. 这次改造在简历上怎么写

可以写成一条：

> 引入 RabbitMQ 将导出任务异步化，接口仅创建任务并投递消息，consumer 异步生成导出快照并更新 `PENDING/RUNNING/SUCCESS/FAILED` 状态，降低长耗时任务对主请求线程的阻塞。

再写一条：

> 基于 Redis 实现账本维度分布式锁与接口幂等控制：按 `bookId` 串行化账单写操作，避免并发修改账本聚合数据的丢更新风险；通过 `setIfAbsent + TTL` 拦截账单重复提交和审批重复操作。

如果篇幅有限，可以合并：

> 引入 RabbitMQ 与 Redis 完成工程化改造：通过消息队列实现导出任务异步化，通过 Redis 分布式锁和幂等 key 处理同账本并发写、重复提交和重复审批问题。

## 16. 当前实现边界

这次改造体现的是工程思想和可运行链路，但仍保留了几个边界：

- 导出结果目前仍是 `snapshot://exports/{id}?bytes=...`，没有真正生成 Excel/PDF 文件。
- 前端尚未改成完整的导出任务轮询 UI。
- Redis 锁目前保护的是写入口；项目当前没有维护独立的账本汇总表，所以它主要是为后续聚合缓存/汇总字段预留并发保护。
- 幂等 key 目前由后端根据业务字段生成，更标准的做法是前端传 `Idempotency-Key` 请求头。

这些边界反而适合面试时讲：

> 当前版本先实现工程链路，后续可以把导出 worker 替换为真实文件生成器，把前端改成任务进度轮询，并为新增账单接口引入客户端生成的 Idempotency-Key。
