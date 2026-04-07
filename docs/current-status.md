# Current Status

## 当前进度

- 当前已推进到：`Day27`
- 当前阶段主题：`自动对账任务第一版`

## 当前已完成

- Day01-Day12：Web3 基础认知、工程设计、钱包后端项目方案
- Day13-Day16：Spring Boot + MyBatis-Plus 工程骨架、ERC-20 充值扫描、确认数推进、正式入账
- Day17-Day20：提现申请、审核、广播、回执同步、`nonce` 预留与重试
- Day21：手动补扫与单用户最小资产对账第一版
- Day22：充值链路 `reorg` 风险识别第一版
- Day23：充值补偿与冲正第一版
- Day24：提现回执超时巡检与异常订单治理第一版
- Day25：`nonce` 锁与单 JVM 串行广播第一版
- Day26：任务幂等、重试上限与失败分层第一版
- Day27：自动对账任务第一版

## 当前主工程状态

- 工程路径：`practice/web3-wallet-backend-demo`
- 当前测试结果：`mvn test` 通过
- 当前测试数量：`58`

## 当前关键能力

- ERC-20 `Transfer event` 扫描、候选充值识别、确认数推进、正式入账
- 提现申请、余额冻结、审核通过、审核拒绝、链上广播、回执同步
- 热钱包 `nonce` 预留、失败重试、单 JVM 串行广播锁保护
- 手动补扫、单用户最小资产对账、异常充值 `reorg` 风险识别
- 异常充值补偿、冲正流水 `DEPOSIT_REORG_DEDUCT`、`REORG_COMPENSATED` 终态推进
- 提现回执超时巡检、`receiptCheckRetryCount` 治理计数、`MANUAL_HANDLE_REQUIRED` 人工处理边界
- 提现广播失败治理：
  - `broadcastRetryCount` 自动重试计数
  - `RETRYABLE / MANUAL_HANDLE_REQUIRED` 失败分层
  - 达到 `withdrawBroadcastMaxRetryCount` 后转人工处理
- 自动对账任务：
  - `account_reconcile_result` 结果落库
  - `account_asset_auto_reconcile` 批次化执行
  - 按用户最近批次 / 按任务批次回查结果
  - 保留完整一致性快照与 `mismatchReason`

## Day27 本次落地

- 新增自动对账结果实体：`AccountReconcileResult`
- 新增自动对账结果表：`account_reconcile_result`
- 新增自动对账任务：`AccountReconcileTask.runOnce()`
- 自动对账扫描范围采用业务痕迹并集：
  - `account_balance`
  - `account_bill`
  - `deposit_record(CREDITED)`
  - `withdraw_order`
- 新增自动对账结果查询服务：`AccountReconcileResultAppService`
- 后台新增接口：
  - `POST /admin/reconcile/task/run`
  - `GET /admin/reconcile/result/latest/{userId}`
  - `GET /admin/reconcile/result/batch/{taskBatchNo}`
- 当前自动对账结果会落完整快照，而不是只记录通过 / 失败

## 下一步

- 进入：`Day28`
- 主题：`审计日志与监控指标第一版`
- 目标：
  - 给充值扫描、提现广播、提现超时巡检、自动对账补审计日志
  - 输出关键任务计数、失败原因和批次维度指标
  - 为告警、排障和 GitHub 展示补齐最小可观测性基础
