# Wallet Backend API List

## 文档目的

这份清单不是 Swagger 导出结果，而是从“面试表达 / GitHub 展示”的角度，把当前主项目已经具备的接口能力按业务链路重新整理出来。

当前主项目：

- `practice/web3-wallet-backend-demo`

---

## 一、地址相关

### `POST /wallet/address`

用途：

- 为用户分配充值地址

适合怎么讲：

- 这是充值链路的起点，用来建立“用户 - 链 - 币种 - 地址”的归属关系

### `GET /wallet/address/{userId}`

用途：

- 查询用户当前充值地址

---

## 二、余额相关

### `GET /account/balance/{userId}`

用途：

- 查询用户余额主表
- 查看可用余额和冻结余额

适合怎么讲：

- 平台资金状态不只看链上地址余额，而是通过内部账本表达可用 / 冻结两个口径

---

## 三、充值相关

### `GET /deposit/list`

用途：

- 查询用户充值记录列表

### `GET /deposit/{txHash}`

用途：

- 按交易哈希查询充值记录

---

## 四、提现相关

### `POST /withdraw/apply`

用途：

- 提交提现申请
- 触发冻结余额

### `GET /withdraw/list`

用途：

- 查询提现单列表

### `GET /withdraw/{requestNo}`

用途：

- 查询指定提现单详情

适合怎么讲：

- 这条链路覆盖了申请、冻结、审核、广播、回执同步和最终扣减 / 回退

---

## 五、充值后台任务相关

### `POST /admin/deposit/scan/run`

用途：

- 手动触发一轮 ERC-20 充值扫描

### `POST /admin/deposit/rescan/run`

用途：

- 手动补扫指定区块区间的充值日志

### `POST /admin/deposit/reorg/check/run`

用途：

- 手动触发一轮充值链路 `reorg` 风险检查

### `POST /admin/deposit/compensate/run`

用途：

- 手动触发一轮异常充值补偿与冲正

### `GET /admin/scan/progress/{taskName}`

用途：

- 查询扫描任务当前断点

适合怎么讲：

- 充值任务不是只扫链上日志，还支持断点续跑、补扫、风险识别和补偿治理

---

## 六、提现后台任务相关

### `POST /admin/withdraw/review/approve`

用途：

- 审核通过提现单

### `POST /admin/withdraw/review/reject`

用途：

- 审核拒绝提现单

### `POST /admin/withdraw/broadcast/run`

用途：

- 手动触发一轮提现广播任务

### `POST /admin/withdraw/receipt/sync`

用途：

- 手动触发一轮提现回执同步任务

### `POST /admin/withdraw/timeout/check/run`

用途：

- 手动触发一轮提现回执超时巡检任务

适合怎么讲：

- 提现链路的重点在于冻结、审核、`nonce` 治理、回执同步和超时异常处理

---

## 七、对账相关

### `GET /admin/reconcile/account/{userId}`

用途：

- 查询指定用户的单资产最小对账结果

### `POST /admin/reconcile/task/run`

用途：

- 手动触发一轮自动对账任务

### `GET /admin/reconcile/result/latest/{userId}`

用途：

- 查询指定用户最近一轮自动对账结果

### `GET /admin/reconcile/result/batch/{taskBatchNo}`

用途：

- 查询指定批次号下的自动对账结果

适合怎么讲：

- 这里体现的是“对账从查询能力升级成任务能力，并且支持历史结果追踪”

---

## 八、可观测性相关

### `GET /admin/task-observability/audit/{taskName}`

用途：

- 查询指定后台任务最近审计日志

### `GET /admin/task-observability/metrics/overview`

用途：

- 查询核心后台任务的指标概览

适合怎么讲：

- 任务不仅能执行，还能被追踪、被排障、被展示

---

## 当前接口背后最值得讲的 4 条能力主线

1. 地址分配、充值识别、确认数推进和正式入账组成了完整入金链路
2. 提现申请、审核、广播、回执同步和异常治理组成了完整出金链路
3. 自动对账把账本、流水、充值和提现串成了账实一致主线
4. 审计日志与指标概览让任务治理具备了可观测性表达

---

## 建议配合阅读

- [钱包后端模块设计](./wallet-backend-module-design.md)
- [面试亮点整理](./interview-highlights.md)
- [项目介绍与面试话术](./project-pitch.md)
