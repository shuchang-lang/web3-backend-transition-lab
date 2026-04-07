# Day 26 - 任务幂等、重试上限与失败分层第一版

今天进入 `Day26`。

如果说：

- `Day24` 解决的是“已广播订单长时间没有最终回执时怎么治理”
- `Day25` 解决的是“同一时间多轮广播任务怎么保护热钱包 `nonce` 顺序”

那么 `Day26` 要补的，就是一个更像真实资金后端的问题：

`同一个系统里不同后台任务，怎么统一定义幂等、自动重试边界、失败分层和转人工规则`

这一步对 `钱包后端 / 交易所后端 / 链上数据后端` 都非常关键。
因为真正成熟的后端系统，不只是“会跑任务”，而是要回答清楚：

1. 任务重复执行会不会把数据弄乱
2. 自动重试最多到什么程度
3. 哪些错误应该继续自动跑
4. 哪些错误应该停止自动化并转人工

---

## 一、为什么 Day26 很重要

对于有 `8 年 Java 后端` 经验的人来说，Day26 其实是在把你原本熟悉的这些概念，翻译成 Web3 语境：

- `幂等`：同一条链上事件、同一笔订单、同一笔补偿动作，重复执行不能重复记账
- `重试上限`：任务失败不是无限重跑，而是要有明确上限
- `失败分层`：不是所有失败都应该等下一轮自动恢复
- `人工介入`：系统要知道自己什么时候该停下来，什么时候该把问题交给人

很多 Web3 项目会在“主流程能跑”之后卡住，不是因为不会调用链，而是因为缺少任务治理能力。

也就是：

`能扫链 != 能治理任务`

`能发交易 != 能治理异常`

Day26 就是在补这部分工程化厚度。

---

## 二、Day26 第一版的边界

如果把 Day26 一次性做满，很容易扩成一个大工程，例如：

- 全任务中心
- 统一任务元数据表
- 分布式重试调度
- 死信队列
- 失败告警
- 运维认领机制

但在当前这个学习项目里，第一版更合理的做法是：

1. 先把“统一治理规则”文档化
2. 再挑一条最有代表性的真实链路落代码
3. 让 README、测试、对外查询结果也跟着同步

这次我选择落地的是：

`提现广播失败治理`

因为它最能体现 Web3 资金后端的特殊性：

- 有热钱包 `nonce`
- 有链上广播
- 有节点 / 网络错误
- 有链上参数类错误
- 有“继续自动重试”和“必须人工介入”的明显分界

---

## 三、统一任务治理规则怎么理解

Day26 第一版先把当前项目里的关键任务统一拉进同一套表达框架。

### 1. DepositScanTask

任务目标：

- 扫描 ERC-20 `Transfer event`
- 识别平台充值地址
- 落库候选充值并推进确认数

幂等承载：

- `chain_scan_progress(chain, task_name)`
- `deposit_record(tx_hash, log_index)` 唯一键

自动重试边界：

- 当前默认按“下一轮扫描自然重跑”处理
- 不单独维护 retry count

失败分层：

- 大多数失败属于 `RETRYABLE`
- 比如 RPC 临时失败、节点波动、扫描窗口拉取异常

为什么天然更适合自动重跑：

- 充值扫描本身是“按区块窗口回放”的任务
- 只要 `deposit_record(tx_hash, log_index)` 保证幂等，下一轮重新扫不会重复入账

### 2. DepositRescanTask

任务目标：

- 手动指定区块区间回放充值事件

幂等承载：

- 同样依赖 `deposit_record(tx_hash, log_index)`

自动重试边界：

- 由人工重新触发区间补扫

失败分层：

- 当前仍以 `RETRYABLE` 为主

关键点：

- 手动补扫不会污染正常扫描断点
- 这本身也是任务治理的一部分

### 3. WithdrawExecutionTask.runBroadcastOnce

任务目标：

- 处理 `PENDING_BROADCAST` 的提现订单
- 预留或复用 `nonce`
- 发起 ERC-20 提现广播

幂等承载：

- `requestNo`
- 订单状态机
- 已预留的 `nonce`

自动重试边界：

- 新增 `broadcastRetryCount`
- 新增 `withdrawBroadcastMaxRetryCount`

失败分层：

- `RETRYABLE`
- `MANUAL_HANDLE_REQUIRED`

这是 Day26 真正落代码的主链路。

### 4. WithdrawExecutionAppService.syncSubmittedReceipts

任务目标：

- 同步 `BROADCAST_SUBMITTED` 订单的链上回执

幂等承载：

- 提现订单状态机
- 成功时正式扣减冻结余额
- 失败时执行解冻回退

自动重试边界：

- 当前不单独维护新 retry count
- 默认由下一轮回执同步任务继续处理

### 5. WithdrawTimeoutCheckTask

任务目标：

- 巡检长时间没有最终回执的提现订单

幂等承载：

- 订单状态机
- `receiptCheckRetryCount`

自动重试边界：

- `withdrawReceiptMaxTimeoutRetryCount`

失败分层：

- 继续等待
- 打回 `PENDING_BROADCAST`
- 转 `MANUAL_HANDLE_REQUIRED`

这部分是 Day24 已经落好的治理基础。

### 6. DepositCompensationTask

任务目标：

- 对 `REORG_SUSPECTED` 且已入账的异常充值做冲正

幂等承载：

- `deposit_record.status != REORG_COMPENSATED`
- `account_bill(biz_type, biz_id)` 唯一约束

失败分层：

- 当前先按“任务下轮继续处理”表达

这说明：

`Web3 任务治理的核心，不只是有没有 retry，而是有没有稳定的幂等承载点`

---

## 四、这次代码真正补了什么

### 1. 新增任务失败分层常量

新增文件：

- `TaskFailureLevelConstants`

当前定义：

- `RETRYABLE`
- `MANUAL_HANDLE_REQUIRED`

这样做的好处是，Day26 不再把“失败只是字符串说明”散落在各个 if/else 里，而是开始有统一的任务治理语言。

### 2. 提现广播最大自动重试配置

新增配置：

- `wallet.web3.withdraw-broadcast-max-retry-count`

环境变量：

- `WEB3_WITHDRAW_BROADCAST_MAX_RETRY_COUNT`

默认值：

- `3`

这个值的含义是：

`广播失败后，系统最多允许自动重试多少次；超过上限后，不再继续自动化，而是转人工处理`

### 3. 提现订单新增广播失败治理计数

数据库表 `withdraw_order` 新增字段：

- `broadcast_retry_count`

领域对象 `WithdrawOrder` 新增字段：

- `broadcastRetryCount`

它表示：

`这笔提现订单在广播阶段已经自动重试失败过多少次`

这一步非常关键，因为没有计数，就无法回答：

`这是一笔第一次失败的订单，还是已经反复失败很多次的订单`

### 4. 提现广播失败动作被分成两类

`WithdrawOrder` 当前新增或补强了两个动作：

- `markBroadcastRetryableFailure(...)`
- `markBroadcastManualHandleRequired(...)`

其中：

- 可重试失败会保留在 `PENDING_BROADCAST`
- 人工级失败会推进到 `MANUAL_HANDLE_REQUIRED`
- 两类失败都会累计 `broadcastRetryCount`

这意味着当前系统不再是“广播失败统一等下一轮再说”，而是开始显式地区分治理策略。

### 5. WithdrawExecutionAppService 真正落了 Day26 规则

当前 `broadcastPendingOrders()` 已补上：

- 失败原因提取
- 失败分层判断
- 最大重试上限判断
- 对应状态推进

也就是说，Day26 不再只是文档里写“可以做失败分层”，而是已经真正进了主执行链路。

---

## 五、当前广播失败分层规则

Day26 第一版先用错误文案做最小分类。

### 1. 直接转人工的错误

当前命中以下文案时，直接归为 `MANUAL_HANDLE_REQUIRED`：

- `nonce too low`
- `insufficient funds`
- `invalid sender`
- `replacement transaction underpriced`

为什么这些错误更适合人工级处理：

#### `nonce too low`

这通常说明：

- 本地预留的 `nonce` 已经落后于链上真实状态
- 或者链上 / 节点侧已经有同序号交易

这不是简单“等下一轮任务重跑”就一定能解释清楚的问题。

#### `insufficient funds`

这说明：

- 热钱包余额不足
- gas 资金不足
- 或者金额 / 手续费策略需要重新检查

继续自动重试大概率只会重复失败。

#### `invalid sender`

这通常意味着：

- 私钥与发送地址不匹配
- 签名配置有问题
- 链 ID / 签名参数可能异常

这类错误本质上是配置或签名层问题，不适合盲目自动重试。

#### `replacement transaction underpriced`

这说明：

- 链上已经存在同 nonce 交易
- 当前替换策略不成立

继续自动重跑很可能只会放大问题，需要人工判断是提高手续费、确认原交易状态，还是调整顺序。

### 2. 其余错误先按可重试处理

比如：

- RPC 超时
- 节点抖动
- 临时网络失败
- 上游网关短时异常

这些错误当前统一按 `RETRYABLE` 处理。

也就是说：

- 保持在 `PENDING_BROADCAST`
- `broadcastRetryCount + 1`
- 下轮广播任务继续复用原 `nonce` 尝试

这很像传统 Java 后端里的：

`下游暂时不可用 -> 允许有限次数重试`

---

## 六、达到重试上限后怎么处理

Day26 第一版的核心不是“有重试”，而是：

`重试必须有上限`

当前规则是：

1. 如果本次错误本身是人工级错误
   - 直接转 `MANUAL_HANDLE_REQUIRED`
2. 如果本次错误是可重试错误
   - 先看 `broadcastRetryCount + 1` 是否达到 `withdrawBroadcastMaxRetryCount`
3. 如果达到上限
   - 也转 `MANUAL_HANDLE_REQUIRED`
4. 只有未达到上限时
   - 才继续保留在 `PENDING_BROADCAST`

这一步非常重要，因为它体现了成熟后端系统的一条原则：

`系统要能自动恢复，但不能无限自动恢复`

---

## 七、为什么还要把治理计数暴露给查询接口

Day26 这次不仅改了执行链路，还同步改了：

- `WithdrawOrderResponse`
- `WithdrawOrderAppService.toResponse(...)`

现在对外查询提现订单时，已经可以看到：

- `broadcastRetryCount`
- `receiptCheckRetryCount`

这件事很值钱，因为它带来两个直接收益。

### 1. 后台排障更直观

你再看一笔订单时，不只是看到它处在：

- `PENDING_BROADCAST`
- `BROADCAST_SUBMITTED`
- `MANUAL_HANDLE_REQUIRED`

还可以知道：

- 它已经广播失败过几次
- 它是否已经发生过超时回执重试

### 2. GitHub 展示和面试表达更完整

你可以明确讲：

`这个项目不是只有状态机，还把治理计数一起对外暴露，方便后台排障和运维判断`

这会比“我有提现订单查询接口”更像真实后端项目。

---

## 八、Day26 第一版的工程价值

对你这种准备尽快切 `钱包后端 / 交易所后端 / 链上数据后端` 的背景来说，Day26 特别加分的地方在于：

### 1. 你开始从“写业务流程”升级到“治理后台任务”

很多人能写：

- 扫链
- 记账
- 发交易

但 Day26 体现的是另一层能力：

- 判断哪些任务天然幂等
- 判断哪些失败值得自动重试
- 判断什么时点必须转人工

### 2. 你把 Web3 问题翻译成了后端治理问题

例如：

- `nonce too low`
- `insufficient funds`
- `replacement transaction underpriced`

这些表面上是 Web3 错误，但 Day26 关心的是：

`这些错误在任务系统里应该落到哪一层治理动作`

这就是很典型的高级后端思维。

### 3. 你开始有“统一规则 + 关键链路落地”的意识

Day26 没有贸然做一个大而全的任务中心，而是选择：

- 先统一规则
- 再挑最关键链路落代码

这是很成熟的迭代方式。

---

## 九、这次测试覆盖了什么

本次新增 / 调整的测试重点包括：

1. 广播失败是可重试错误时
   - 订单保持 `PENDING_BROADCAST`
   - `broadcastRetryCount + 1`
2. 广播失败命中人工级错误时
   - 订单直接转 `MANUAL_HANDLE_REQUIRED`
3. 广播失败虽然是可重试错误，但达到最大自动重试次数时
   - 订单也转 `MANUAL_HANDLE_REQUIRED`
4. 所有受 `WalletWeb3Properties` 新参数影响的测试构造器全部同步修正
5. 所有受 `WithdrawOrderResponse` 新字段影响的控制器测试全部同步修正

当前在 `practice/web3-wallet-backend-demo` 下执行：

```bash
mvn test
```

结果是：

- `51` 个测试全部通过

---

## 十、Day26 之后，项目到了什么阶段

到 Day26 为止，这个学习项目已经不只是：

- 会扫充值
- 会做提现
- 会处理回执

而是开始具备下面这层更真实的工程能力：

- 任务幂等承载点清晰
- 重试次数不是无限制
- 失败开始被分层治理
- 系统知道什么时候该继续自动恢复
- 系统也知道什么时候该停止自动化并转人工

这正是资金后端项目非常重要的可信度来源。

---

## 十一、进入 Day27 前，应该怎么理解下一步

Day26 解决的是：

`任务失败以后怎么治理`

而 `Day27` 更自然的下一步是：

`资产结果怎么定时检查，并把检查结果沉淀成可追踪的数据`

所以 Day27 做“自动对账任务第一版”是非常顺的。

因为当前项目已经有了：

- `account_balance`
- `account_bill`
- 充值已入账数据
- 提现冻结与成功扣减数据
- 手动对账能力

接下来最自然的升级就是：

1. 把手动对账升级成自动任务
2. 把对账结果落库
3. 让系统能长期表达“哪些资产一致、哪些不一致、为什么不一致”

也就是说：

`Day26 治理的是任务执行过程`

`Day27 治理的是资产结果一致性`

这两个阶段连在一起，项目就会越来越像真正的资金后端系统。
