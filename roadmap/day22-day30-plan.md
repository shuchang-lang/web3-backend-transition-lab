# Day22-Day30 Plan

当前项目已经推进到 `Day29`。

到这里为止，主项目已经完成：

- 充值扫描、确认数推进、正式入账
- 提现申请、审核、广播、回执同步、失败回退
- 热钱包 `nonce` 预留与失败后复用
- 手动补扫与单用户最小资产对账
- 充值链路 `reorg` 风险识别
- 异常充值补偿与冲正
- 提现回执超时巡检与异常订单治理
- 单 JVM 串行广播锁保护
- 任务幂等、重试上限与失败分层第一版
- 自动对账任务与结果落库第一版
- 审计日志与监控指标第一版
- GitHub 展示与面试表达强化

Day22-Day30 这一段的目标，不再是单纯堆“新接口”，而是把项目往下面三个方向推进：

1. 更像真实资金后端
2. 更适合 GitHub 展示
3. 更能支撑 `钱包后端 / 交易所后端 / 链上数据后端` 岗位面试表达

---

## 总体原则

### 1. 先做治理能力，再做花活

这个仓库已经足够说明你会：

- 接链
- 扫日志
- 做充值
- 做提现

接下来更值钱的是继续补齐：

- `reorg`
- `compensation`
- `reconcile`
- `task governance`
- `observability`

### 2. 始终围绕资金后端主线

Day22-Day30 仍然聚焦：

- `wallet backend`
- `exchange backend`
- `chain-data backend`

不把重心切去纯合约开发、复杂前端或投机方向。

### 3. 每一天都要有可展示产出

每一阶段尽量至少落下其中两类内容：

1. 代码
2. 测试
3. 文档
4. README / 图示 / 面试表达

---

## 已完成阶段

### Day22：充值链路 reorg 风险识别第一版

目标：

- 识别已扫描充值记录对应的链上交易或日志是否异常
- 把风险先显式标记出来

当前状态：

- 已完成

产出：

- `REORG_SUSPECTED` 风险识别
- 巡检任务与测试
- 对应 Day22 文档

### Day23：充值补偿与冲正第一版

目标：

- 对异常且已入账的充值做最小补偿闭环

当前状态：

- 已完成

产出：

- `DEPOSIT_REORG_DEDUCT`
- `REORG_COMPENSATED`
- 补偿任务与测试
- 对应 Day23 文档

### Day24：提现回执超时巡检与异常订单治理第一版

目标：

- 治理 `BROADCAST_SUBMITTED` 长时间没有最终结果的订单

当前状态：

- 已完成

产出：

- `receiptCheckRetryCount`
- `MANUAL_HANDLE_REQUIRED`
- 超时巡检任务
- 对应 Day24 文档

### Day25：nonce 锁与串行广播第一版

目标：

- 避免同一时间多轮广播任务竞争热钱包 `nonce`

当前状态：

- 已完成

产出：

- `WithdrawBroadcastLock`
- 单 JVM 串行广播保护
- 锁占用跳过语义
- 对应 Day25 文档

### Day26：任务幂等、重试上限与失败分层第一版

目标：

- 把当前项目里的关键后台任务统一拉进“幂等、重试、失败分层、转人工”这套治理语言

当前状态：

- 已完成

第一版边界：

- 统一规则通过文档收口
- 代码真正落在“提现广播失败”主链路

本次关键产出：

- `TaskFailureLevelConstants`
- `broadcastRetryCount`
- `withdrawBroadcastMaxRetryCount`
- 提现广播失败 `RETRYABLE / MANUAL_HANDLE_REQUIRED`
- 超过最大自动重试次数后转人工
- 对外查询订单时暴露治理计数

### Day27：自动对账任务第一版

目标：

- 把 Day21 的手动对账推进成自动任务
- 让对账结果具备批次化沉淀和历史追踪能力

当前状态：

- 已完成

本次关键产出：

- `account_reconcile_result`
- `AccountReconcileTask`
- `AccountReconcileResultAppService`
- `POST /admin/reconcile/task/run`
- `GET /admin/reconcile/result/latest/{userId}`
- `GET /admin/reconcile/result/batch/{taskBatchNo}`
- `mvn test` 通过 `58` 个测试

---

## 接下来要做的阶段

### Day28：审计日志与监控指标第一版

主题：

- 让系统具备最小可观测性

目标：

1. 对关键任务保留审计日志
2. 输出关键计数和失败原因
3. 为后续告警和排障打基础

建议落点：

- 充值扫描任务
- 提现广播任务
- 提现超时巡检任务
- 对账任务

当前状态：

- 已完成

本次关键产出：

- `task_audit_log`
- `TaskAuditLogAppService`
- `AdminTaskObservabilityController`
- `GET /admin/task-observability/audit/{taskName}`
- `GET /admin/task-observability/metrics/overview`
- `mvn test` 通过 `62` 个测试

### Day29：GitHub 展示与面试表达强化

主题：

- 把工程内容翻译成别人一眼能看懂的价值

目标：

1. 强化 README 项目亮点与主链路表达
2. 补充模块职责、状态机和治理规则总结
3. 整理一版高频面试问答

当前状态：

- 已完成

本次关键产出：

- `design/project-pitch.md`
- `design/interview-highlights.md`
- `design/wallet-backend-module-design.md`
- `design/wallet-backend-api-list.md`
- `docs/day29-github-showcase-and-interview-story.md`

### Day30：项目收口与简历表达第一版

主题：

- 把仓库收口成一段可以写进简历、也能在面试里讲顺的项目经历

目标：

1. 梳理项目背景、职责、技术栈、关键难点
2. 总结资金一致性、链上异常治理、任务治理、可观测性等亮点
3. 形成一版可直接复述的项目介绍材料

---

## 优先级建议

如果时间有限，优先保证下面 6 个节点：

1. `Day24`：提现异常订单治理
2. `Day25`：热钱包 nonce 串行保护
3. `Day26`：任务幂等、重试上限与失败分层
4. `Day27`：自动对账任务
5. `Day28`：审计日志与监控指标
6. `Day29`：GitHub 展示与面试表达强化

因为这六步连起来，已经能形成一条比较完整、也比较有说服力的资金后端工程主线：

`充值 -> 提现 -> 异常治理 -> 任务治理 -> 自动对账 -> 可观测性 -> 项目表达`

---

## 当前承接关系

Day26 解决的是：

- 任务失败以后怎么分层治理

Day27 解决的是：

- 资产结果怎么被批次化检查并沉淀成可追踪数据

Day28 要解决的是：

- 这些任务怎么进一步变成“可观测、可排障、可告警”的系统能力

Day29 要解决的是：

- 怎么把前面这些工程能力翻译成 GitHub 和面试都容易理解的表达

Day30 要解决的是：

- 怎么把这些表达进一步收口成简历和项目经历材料

这五天连在一起，项目会明显从“能跑主流程”继续进入“开始具备真实后端治理能力，并且更适合展示、讲述和写进简历”的阶段。
