# Day27：自动对账任务第一版

## 今天要解决什么问题

Day21 我们已经做了“单用户、单资产、手动触发”的最小对账能力，但它更像一个排障工具，还不是一个真正有后端治理味道的任务。

到了 Day27，需要把这件事往前再推一步：

1. 把手动对账包装成后台任务
2. 让任务结果可落库、可追踪、可回看
3. 让“资产主表 / 流水 / 充值 / 提现”之间的一致性检查具备批次概念

这一步对 `钱包后端 / 交易所后端 / 链上数据后端` 的面试表达很重要，因为对账本身就是资金系统里非常典型、也非常容易体现后端治理能力的主题。

---

## 为什么 Day27 要接在 Day21-Day26 后面

Day21 做的是：

- 手动补扫
- 单用户最小资产对账

Day22-Day23 做的是：

- 充值 `reorg` 风险识别
- 异常充值补偿与冲正

Day24-Day26 做的是：

- 提现超时巡检
- 任务幂等、重试上限、失败分层

所以 Day27 的承接关系非常自然：

- 前面已经有了资金状态、异常治理和任务化表达
- 现在需要补上“定期检查账实是否一致”的闭环

换句话说，Day27 让这个项目从“主流程能跑”进一步进入“开始具备真实资金后端治理能力”。

---

## Day27 第一版设计取舍

### 1. 先做任务化，不急着直接上 `@Scheduled`

这一版仍然延续当前项目风格：

- 提供 `runOnce()` 任务入口
- 通过后台接口手动触发
- 为后续接入真正定时调度预留稳定实现

这样做的好处是：

- 更容易测试
- 更容易在 GitHub 上展示
- 更方便先把对账规则、结果结构和批次表达做扎实

### 2. 自动对账扫描“业务痕迹并集”，不只扫余额主表

本次任务会把下面四类表里的 `userId + chain + tokenSymbol` 汇总成资产目标集合：

- `account_balance`
- `account_bill`
- `deposit_record` 中已正式入账的数据
- `withdraw_order`

这样做是为了避免只看 `account_balance` 时漏掉“有业务痕迹但余额主表还没落下完整状态”的资产。

### 3. 结果表保存完整快照，而不是只存一个通过 / 失败

本次新增 `account_reconcile_result`，会把下面这些字段一并沉淀下来：

- 当前可用 / 冻结 / 总余额
- 已入账充值总额
- 已成功提现总额
- 待完成提现冻结总额
- 资产口径流水净额
- 最新流水快照
- 各项一致性布尔结果
- `mismatchReason`

这样做的好处是：

- 后台排障时不需要二次重算
- README / GitHub 展示更直观
- 后续做监控、审计、告警时也更容易复用

### 4. “最近结果”按批次返回，而不是按单条记录返回

新增“按用户查询最近自动对账结果”时，不是简单拿最新一条，而是：

1. 先找到该用户最近一次任务批次
2. 再返回这个批次下该用户的全部资产结果

这样可以避免同一个用户不同资产混入不同批次的数据。

---

## 本次代码落地

### 1. 新增结果落库实体与表结构

新增：

- `practice/web3-wallet-backend-demo/src/main/java/com/web3lab/wallet/domain/account/AccountReconcileResult.java`
- `practice/web3-wallet-backend-demo/src/main/java/com/web3lab/wallet/infrastructure/persistence/AccountReconcileResultMapper.java`

表结构新增：

- `account_reconcile_result`

### 2. 新增自动对账任务

新增：

- `practice/web3-wallet-backend-demo/src/main/java/com/web3lab/wallet/application/account/AccountReconcileTask.java`

核心任务名：

- `account_asset_auto_reconcile`

核心逻辑：

1. 聚合本轮资产目标
2. 逐个调用 Day21 的 `AccountReconcileAppService.reconcile(...)`
3. 把结果快照落库
4. 返回本轮批次号、一致数量、不一致数量

### 3. 新增自动对账结果查询服务

新增：

- `practice/web3-wallet-backend-demo/src/main/java/com/web3lab/wallet/application/account/AccountReconcileResultAppService.java`

### 4. 扩展后台接口

更新：

- `practice/web3-wallet-backend-demo/src/main/java/com/web3lab/wallet/controller/AdminReconcileController.java`

新增接口：

- `POST /admin/reconcile/task/run`
- `GET /admin/reconcile/result/latest/{userId}`
- `GET /admin/reconcile/result/batch/{taskBatchNo}`

保留已有手动单资产对账接口：

- `GET /admin/reconcile/account/{userId}`

### 5. 补充测试

新增测试：

- `AccountReconcileTaskTest`
- `AccountReconcileResultAppServiceTest`

扩展测试：

- `WalletControllersTest`

---

## 这一版解决了什么

到 Day27 为止，这个项目里的对账能力已经从“手动排查工具”升级成了“可任务化执行的最小治理能力”：

1. 可以手动触发一整轮自动对账
2. 可以拿到批次号
3. 可以回查某个批次的完整结果
4. 可以按用户查看最近一轮对账快照
5. 可以直接看到差异原因，而不是只知道失败

这在面试里可以很自然地表达成：

- 我们不是只有主流程，还补了资金一致性治理
- 对账任务会沉淀历史快照，而不是只在接口里临时算
- 后续可以继续往审计、监控、告警方向扩展

---

## 当前边界

Day27 第一版仍然有意保持克制：

- 还没有真正接 `@Scheduled`
- 还没有把结果推到监控指标或告警系统
- 还没有做更细粒度的人工处理工单流转
- 还没有按多链、多币种大规模场景优化扫描性能

这不是缺陷，而是有意把“任务化 + 落库 + 查询 + 展示”先做成最小稳定闭环。

---

## 验证结果

在 `practice/web3-wallet-backend-demo` 下执行：

```bash
mvn test
```

当前结果：

- `58` 个测试全部通过

---

## 下一步

下一步进入：

- `Day28：审计日志与监控指标第一版`

承接方向会很自然：

1. 给关键任务补审计日志
2. 给充值扫描、提现广播、超时巡检、自动对账输出关键计数
3. 为告警和排障准备最小可观测性基础
