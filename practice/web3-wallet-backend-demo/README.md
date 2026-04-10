# Web3 Wallet Backend Demo

从 `Day13` 开始落地的最小 Spring Boot 钱包后端项目。

当前已经推进到：

- `Day30：项目收口与简历表达第一版`

这个子项目的定位不是完整生产系统，而是一个足够真实、足够可展示、足够能讲清楚、也足够能写进简历的 Web3 资金后端最小闭环。

---

## 一句话怎么讲

如果要快速介绍这个子项目，可以直接说：

> 这是一个用 Java 后端方式实现的 Web3 钱包资金后端 demo，覆盖充值监听、提现执行、内部账本、异常治理、自动对账和任务可观测性，目标是把链上结果稳定翻译成平台内部账务。

---

## 如果要写进简历

可以先用这句：

> 基于 Java 17、Spring Boot、MyBatis-Plus、MySQL 和 web3j 独立设计并实现 EVM 钱包资金后端实践项目，覆盖 ERC-20 充值扫描、提现执行、内部账本、`reorg` 补偿、自动对账与任务可观测性。

如果需要一行版、标准版、职责亮点版和岗位定制版写法，优先配合：

- [简历项目经历材料](../../design/resume-project-story.md)

---

## 当前已实现能力

- Spring Boot + Maven + Java 17 基础工程
- MyBatis-Plus + MySQL 持久化
- SpringDoc + Javadoc 零侵入接口文档
- 地址分配与查询
- 余额查询
- ERC-20 `Transfer event` 扫描
- 候选充值识别、确认数推进、正式入账
- 充值 `reorg` 风险识别
- 异常充值补偿与冲正
- 提现申请、余额冻结、审核通过、审核拒绝
- 提现链上广播、回执同步、成功扣减、失败回退
- 热钱包 `nonce` 预留、失败后复用同一 `nonce`
- 单 JVM 串行广播锁保护
- 已广播订单超时巡检与人工处理边界
- 提现广播失败治理：
  - `broadcastRetryCount`
  - `receiptCheckRetryCount`
  - `RETRYABLE / MANUAL_HANDLE_REQUIRED`
  - 最大自动重试次数 `withdrawBroadcastMaxRetryCount`
- 自动对账任务、对账结果快照落库与历史回查
- 统一任务审计日志、指标概览与后台可观测性查询

---

## 当前接口

- `POST /wallet/address`
- `GET /wallet/address/{userId}`
- `GET /account/balance/{userId}`
- `GET /deposit/list`
- `GET /deposit/{txHash}`
- `POST /admin/deposit/scan/run`
- `POST /admin/deposit/rescan/run`
- `POST /admin/deposit/reorg/check/run`
- `POST /admin/deposit/compensate/run`
- `GET /admin/scan/progress/{taskName}`
- `GET /admin/reconcile/account/{userId}`
- `POST /admin/reconcile/task/run`
- `GET /admin/reconcile/result/latest/{userId}`
- `GET /admin/reconcile/result/batch/{taskBatchNo}`
- `GET /admin/task-observability/audit/{taskName}`
- `GET /admin/task-observability/metrics/overview`
- `POST /withdraw/apply`
- `GET /withdraw/list`
- `GET /withdraw/{requestNo}`
- `POST /admin/withdraw/review/approve`
- `POST /admin/withdraw/review/reject`
- `POST /admin/withdraw/broadcast/run`
- `POST /admin/withdraw/receipt/sync`
- `POST /admin/withdraw/timeout/check/run`

---

## 接口文档

项目已接入 `SpringDoc`，并按“使用 Javadoc 生成接口说明、尽量不写 Swagger 注解”的方式组织接口文档。

应用启动后可访问：

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

---

## 默认业务假设

- 单链：`ETH_SEPOLIA`
- 单币种：`USDT`
- 地址分配阶段先使用 demo 地址生成器，不直接依赖真实钱包服务
- 数据库直接连接 `MySQL`，不使用内存数据库
- 未配置真实 `RPC` 时，充值扫描任务只初始化扫描进度，不真正拉链上事件
- 未配置提现热钱包私钥时，提现广播任务会自动跳过
- 当前最小版本假设 `amount` 和 `fee` 使用同一币种冻结与扣减
- 广播失败后会优先复用订单中已预留的 `nonce`
- Day25 只解决单 JVM 维度的串行广播保护，尚未升级到多实例分布式锁
- Day26 开始把提现广播失败分成两类：
  - 可重试错误：保留在 `PENDING_BROADCAST`
  - 人工级错误：直接转 `MANUAL_HANDLE_REQUIRED`
- Day27 自动对账任务会聚合余额主表、流水、已入账充值、提现订单里的业务痕迹
- 查询“用户最近自动对账结果”时，返回的是最近一次批次下的全部资产结果
- Day28 可观测性第一版当前重点覆盖 4 个关键任务：
  - 充值扫描
  - 提现广播
  - 提现回执超时巡检
  - 自动对账
- 当前人工级错误识别包含：
  - `nonce too low`
  - `insufficient funds`
  - `invalid sender`
  - `replacement transaction underpriced`

---

## 本地运行前准备

1. 创建数据库，例如 `web3_wallet_demo`
2. 配置环境变量

基础数据库环境变量：

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`

Web3 相关环境变量：

- `WEB3_RPC_URL`
- `WEB3_DEPOSIT_TOKEN_CONTRACT`
- `WEB3_SCAN_START_BLOCK`
- `WEB3_SCAN_STEP`
- `WEB3_TOKEN_DECIMALS`
- `WEB3_CONFIRMATIONS_THRESHOLD`
- `WEB3_WITHDRAW_HOT_WALLET_PRIVATE_KEY`
- `WEB3_WITHDRAW_CHAIN_ID`
- `WEB3_WITHDRAW_GAS_LIMIT`
- `WEB3_WITHDRAW_BROADCAST_MAX_RETRY_COUNT`
- `WEB3_WITHDRAW_RECEIPT_TIMEOUT_MINUTES`
- `WEB3_WITHDRAW_RECEIPT_MAX_TIMEOUT_RETRY_COUNT`

MySQL 默认值：

- `MYSQL_HOST=localhost`
- `MYSQL_PORT=3306`
- `MYSQL_DATABASE=web3_wallet_demo`
- `MYSQL_USERNAME=root`
- `MYSQL_PASSWORD=root`

扫描与提现示例值：

- `WEB3_RPC_URL=https://sepolia.infura.io/v3/your-key`
- `WEB3_DEPOSIT_TOKEN_CONTRACT=0xYourTokenContract`
- `WEB3_SCAN_START_BLOCK=7000000`
- `WEB3_SCAN_STEP=200`
- `WEB3_TOKEN_DECIMALS=6`
- `WEB3_CONFIRMATIONS_THRESHOLD=6`
- `WEB3_WITHDRAW_HOT_WALLET_PRIVATE_KEY=0xyour_private_key`
- `WEB3_WITHDRAW_CHAIN_ID=11155111`
- `WEB3_WITHDRAW_GAS_LIMIT=120000`
- `WEB3_WITHDRAW_BROADCAST_MAX_RETRY_COUNT=3`
- `WEB3_WITHDRAW_RECEIPT_TIMEOUT_MINUTES=10`
- `WEB3_WITHDRAW_RECEIPT_MAX_TIMEOUT_RETRY_COUNT=2`

说明：

- `WEB3_RPC_URL`：EVM RPC 节点地址
- `WEB3_DEPOSIT_TOKEN_CONTRACT`：当前监听和出金使用的 ERC-20 合约地址
- `WEB3_SCAN_START_BLOCK`：首次扫描起始区块
- `WEB3_SCAN_STEP`：单次最多扫描的区块窗口
- `WEB3_TOKEN_DECIMALS`：代币小数位
- `WEB3_CONFIRMATIONS_THRESHOLD`：充值正式入账所需确认数
- `WEB3_WITHDRAW_HOT_WALLET_PRIVATE_KEY`：提现热钱包私钥，仅用于本地最小演示
- `WEB3_WITHDRAW_CHAIN_ID`：提现广播使用的链 ID
- `WEB3_WITHDRAW_GAS_LIMIT`：提现广播 gas limit
- `WEB3_WITHDRAW_BROADCAST_MAX_RETRY_COUNT`：广播失败后的最大自动重试次数
- `WEB3_WITHDRAW_RECEIPT_TIMEOUT_MINUTES`：已广播提现订单等待回执的超时阈值
- `WEB3_WITHDRAW_RECEIPT_MAX_TIMEOUT_RETRY_COUNT`：回执超时后允许打回待广播的最大次数

---

## 本地运行

```bash
mvn spring-boot:run
```

---

## 初始化表结构

可以先执行 [sql/create_database.sql](./sql/create_database.sql) 创建数据库。

应用启动时会自动执行 [schema.sql](./src/main/resources/schema.sql)。

当前 `withdraw_order` 已包含治理相关字段：

- `broadcast_retry_count`
- `receipt_check_retry_count`

Day27 新增的自动对账结果表：

- `account_reconcile_result`

Day28 新增的任务审计日志表：

- `task_audit_log`

---

## 当前主链路

### 充值主链路

1. 手动触发 `POST /admin/deposit/scan/run`
2. 读取或初始化 `chain_scan_progress`
3. 按区块窗口拉取 ERC-20 `Transfer event`
4. 识别平台充值地址
5. 幂等写入 `deposit_record(tx_hash, log_index)`
6. 先标记为 `PENDING_CONFIRM`
7. 按最新区块刷新确认数
8. 达到阈值后更新 `account_balance`
9. 写入 `account_bill`
10. 把 `deposit_record` 推进到 `CREDITED`
11. 更新扫描断点

### 提现主链路

1. 发起 `POST /withdraw/apply`
2. 校验余额主表和可用余额
3. 把 `amount + fee` 从可用余额转入冻结余额
4. 写入 `withdraw_order`
5. 状态先进入 `PENDING_REVIEW`
6. 写入冻结流水 `WITHDRAW_FREEZE`
7. 审核通过后推进到 `PENDING_BROADCAST`
8. 审核拒绝则解冻回退并写入 `WITHDRAW_UNFREEZE`
9. 广播任务为订单预留或复用 `nonce`
10. 广播成功后推进到 `BROADCAST_SUBMITTED`
11. 回执同步任务按链上结果推进到 `SUCCESS / FAILED`
12. 成功写入 `WITHDRAW_DEDUCT`
13. 失败执行解冻回退

### Day24-Day26 治理补充

1. 回执长时间未返回时，由 `POST /admin/withdraw/timeout/check/run` 触发超时巡检
2. 超时后先重查链上回执，再决定继续等待、打回待广播还是转人工
3. 订单会累计 `receiptCheckRetryCount`
4. 广播失败时，订单会累计 `broadcastRetryCount`
5. 人工级错误直接转 `MANUAL_HANDLE_REQUIRED`
6. 可重试错误达到 `WEB3_WITHDRAW_BROADCAST_MAX_RETRY_COUNT` 后也会转人工
7. 查询订单时可直接看到这两个治理计数

### Day27 自动对账补充

1. 手动触发 `POST /admin/reconcile/task/run`
2. 任务会聚合所有存在业务痕迹的资产目标
3. 逐个调用 Day21 的最小对账逻辑
4. 把结果快照写入 `account_reconcile_result`
5. 可通过用户最近批次或指定批次回查结果

### Day28 可观测性补充

1. 关键任务执行后会落一条 `task_audit_log`
2. 审计日志会保留批次号、状态、关键计数和失败原因
3. 指标快照会用 `metricSnapshot` 记录任务特有计数
4. 可通过 `GET /admin/task-observability/audit/{taskName}` 查询最近日志
5. 可通过 `GET /admin/task-observability/metrics/overview` 查看核心任务指标概览

---

## 这个项目最适合讲的亮点

如果面试时只挑几件事讲，优先讲下面 5 个：

1. ERC-20 充值不是直接入账，而是“候选记录 + 确认数达标 + 正式入账”
2. 提现不是直接打款，而是“冻结 -> 审核 -> 广播 -> 回执同步 -> 成功扣减 / 失败回退”
3. `reorg` 风险不是只识别，还继续做了补偿与冲正
4. 自动对账把账本、流水、充值、提现之间的一致性检查任务化了
5. 任务审计日志和指标概览让后台任务具备了最小可观测性

---

## 建议演示顺序

如果要给别人现场演示，可以按这个顺序：

1. 先讲充值主链路
2. 再讲提现主链路
3. 然后讲 `reorg`、补偿和超时巡检等异常治理
4. 再讲自动对账
5. 最后讲任务审计日志和指标概览

这样能让别人从“主流程”一路看到“治理能力”。

---

## 当前测试

当前在本模块执行：

```bash
mvn test
```

已通过 `62` 个测试，覆盖：

- 控制器接口返回
- 候选充值识别与正式入账
- 充值链路 `reorg` 风险识别
- 充值补偿、冲正与负余额风险敞口
- 提现申请、审核、冻结、解冻
- 提现广播、`nonce` 预留、失败重试、回执处理
- 提现回执超时巡检与人工处理边界
- 单 JVM 串行广播锁与锁占用跳过语义
- Day26 新增的广播失败分层与最大重试上限
- Day27 自动对账任务、结果查询与批次表达
- Day28 任务审计日志、指标概览与后台可观测性接口

---

## 下一步

Day30 完成后，下一步进入：

- `第三阶段：让项目更像真实资金后端`

优先会继续往下面 3 个方向推进：

1. 更完整的任务治理表达
2. 多实例场景下的广播锁升级方案
3. 更接近生产的监控、告警、费率、风控与运维治理说明

---

## 相关设计材料

- [简历项目经历材料](../../design/resume-project-story.md)
- [项目介绍与面试话术](../../design/project-pitch.md)
- [面试亮点整理](../../design/interview-highlights.md)
- [模块设计](../../design/wallet-backend-module-design.md)
- [接口清单](../../design/wallet-backend-api-list.md)
