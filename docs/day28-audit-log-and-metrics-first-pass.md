# Day28：审计日志与监控指标第一版

## 今天要解决什么问题

Day27 我们已经有了自动对账任务和结果落库，但系统整体还停留在“任务执行完了，可以手动回查”的阶段。

如果想让这个项目更像真实资金后端，还需要再往前走一步：

1. 关键后台任务执行后要留下统一审计日志
2. 审计日志要能表达批次、状态、关键计数和失败原因
3. 后台要能直接查询任务指标概览，而不是只看控制台日志

这一步对应的是 `observability`，也就是可观测性。对 Java 后端来说，可以把它理解成：

- 不只是让任务“能跑”
- 而是让任务“跑完之后能看见、能追踪、能排障”

---

## 为什么 Day28 要接在 Day27 后面

Day26 做的是：

- 任务失败分层
- 自动重试上限
- 转人工边界

Day27 做的是：

- 自动对账任务
- 对账结果落库
- 批次化结果回查

Day28 自然就该解决：

- 这些任务到底有没有执行
- 最近执行得怎么样
- 哪些任务经常被跳过
- 哪些任务最近失败过
- 某一轮任务的关键计数到底是多少

也就是说，Day28 是把“任务治理”和“结果治理”再往前推进到“可观测治理”。

---

## Day28 第一版设计取舍

### 1. 先做后台审计日志和查询接口，不急着直接上 Prometheus

这一版没有直接接外部指标系统，而是先做：

- `task_audit_log` 统一审计日志表
- 任务执行后自动写入审计日志
- 后台查询最近审计日志
- 后台查询核心任务指标概览

这样做的好处是：

- 对当前 demo 项目更轻量
- 更适合 GitHub 展示
- 更容易和已有后台接口风格保持一致
- 后续如果再接 Prometheus / 告警系统，也有稳定的任务口径可以复用

### 2. 统一审计模型，但允许任务保留自己的指标快照

不同任务的关键计数并不完全一样：

- 充值扫描关心 `fromBlock / toBlock / detectedCount / creditedCount`
- 提现广播关心 `processedCount / updatedCount`
- 超时巡检关心 `continueWaiting / resolved / retryable / manualHandle`
- 自动对账关心 `consistentCount / inconsistentCount`

所以 Day28 第一版采用两层表达：

1. 统一字段：
   - `taskName`
   - `taskBatchNo`
   - `taskStatus`
   - `processedCount`
   - `successCount`
   - `warningCount`
   - `failCount`
   - `failureReason`
   - `remark`
2. 任务特有字段：
   - `metricSnapshot`

这样既方便统一汇总，也保留了任务自己的业务语义。

### 3. 审计日志要独立事务提交

这是这一版里很关键的一个点。

如果审计日志和主任务共用同一个事务，那么任务失败回滚时，日志也会一起回滚，最后就会出现：

- 任务失败了
- 但系统里什么都没留下

所以 `TaskAuditLogAppService.recordSuccess / recordSkipped / recordFailure` 都使用了独立事务，让失败也能留下痕迹。

这和真实后端里“业务事务”和“审计留痕”分离的思路是一致的。

---

## 本次代码落地

### 1. 新增统一任务审计日志结构

新增：

- `practice/web3-wallet-backend-demo/src/main/java/com/web3lab/wallet/domain/task/TaskAuditLog.java`
- `practice/web3-wallet-backend-demo/src/main/java/com/web3lab/wallet/infrastructure/persistence/TaskAuditLogMapper.java`
- `practice/web3-wallet-backend-demo/src/main/java/com/web3lab/wallet/common/task/TaskAuditStatusConstants.java`

表结构新增：

- `task_audit_log`

### 2. 新增任务审计日志应用服务

新增：

- `practice/web3-wallet-backend-demo/src/main/java/com/web3lab/wallet/application/task/TaskAuditLogAppService.java`

主要能力：

- 生成任务批次号
- 记录 `SUCCESS / SKIPPED / FAILED`
- 查询指定任务最近日志
- 汇总核心任务指标概览

### 3. 把 4 个关键任务接入审计日志

本次接入：

- `DepositScanTask`
- `WithdrawExecutionTask.runBroadcastOnce()`
- `WithdrawTimeoutCheckTask`
- `AccountReconcileTask`

每一轮执行现在都会记录：

- 批次号
- 任务状态
- 核心计数
- 指标快照
- 失败原因或跳过原因

### 4. 新增后台可观测性查询接口

新增：

- `practice/web3-wallet-backend-demo/src/main/java/com/web3lab/wallet/controller/AdminTaskObservabilityController.java`

新增接口：

- `GET /admin/task-observability/audit/{taskName}`
- `GET /admin/task-observability/metrics/overview`

其中：

- 审计日志接口用于看最近若干轮执行记录
- 指标概览接口用于看核心任务整体运行画像

### 5. 补充测试

新增测试：

- `TaskAuditLogAppServiceTest`

扩展测试：

- `DepositScanTaskTest`
- `WithdrawExecutionTaskTest`
- `WithdrawTimeoutCheckTaskTest`
- `AccountReconcileTaskTest`
- `WalletControllersTest`

---

## 这一版解决了什么

到 Day28 为止，这个项目里的关键后台任务已经不只是“跑一下”，而是具备了最小可观测性：

1. 每轮任务有批次号
2. 每轮任务有状态
3. 每轮任务有关键计数
4. 跳过和失败都有原因
5. 后台接口能直接查最近日志和指标概览

这在面试里非常好讲，因为它能很自然地把 Java 后端里的经验迁移过来：

- 定时任务不只是写个 `runOnce()`
- 任务要可追踪
- 失败要可回放
- 排障要有最小诊断数据
- 监控指标和审计日志是治理能力的一部分

---

## 当前边界

Day28 第一版仍然保持克制：

- 还没有真正接外部监控系统
- 还没有做 Prometheus 指标暴露
- 还没有做告警规则和告警通知
- 还没有把所有任务都纳入统一观测体系
- 当前重点只覆盖 4 个关键任务

这不是缺陷，而是有意先把“任务留痕 + 查询 + 概览”做成稳定闭环。

---

## 验证结果

在 `practice/web3-wallet-backend-demo` 下执行：

```bash
mvn test
```

当前结果：

- `62` 个测试全部通过

---

## 下一步

下一步进入：

- `Day29：GitHub 展示与面试表达强化`

承接方向会很自然：

1. 强化 README 项目亮点和治理能力表达
2. 补模块职责、状态机和任务治理总结
3. 整理更适合面试讲述的项目话术
