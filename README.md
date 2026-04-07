# Web3 Backend Transition Lab

面向 `8 年 Java 后端工程师` 的 Web3 后端转型实验室。

这个仓库不是单纯的概念笔记，也不是只放几个零散 demo，而是围绕真实求职目标持续推进的一套工程化项目：

- `钱包后端（wallet backend）`
- `交易所后端（exchange backend）`
- `链上数据后端（chain-data backend）`

当前主项目是：

- [practice/web3-wallet-backend-demo](./practice/web3-wallet-backend-demo)

它已经从 Day13 的项目骨架，推进到 `Day29：GitHub 展示与面试表达强化`，并且当前 `mvn test` 已通过 `62` 个测试。

---

## 仓库目标

这个仓库想解决的不是“会不会写 Solidity”，而是下面这些更接近后端岗位面试和真实工作的能力：

1. 能不能把链上概念翻译成后端系统设计。
2. 能不能把充值、提现、账务、补偿、对账、任务治理串成完整链路。
3. 能不能做出一个可运行、可展示、可解释的 Web3 后端项目。
4. 能不能把 Java 后端经验自然迁移到 Web3 语境里。

---

## 快速导航

### 文档与计划

- [docs](./docs)
- [roadmap](./roadmap)
- [design](./design)
- [diagrams](./diagrams)

### 可运行示例

- [practice/web3-wallet-backend-demo](./practice/web3-wallet-backend-demo)
- [practice/web3j-demo](./practice/web3j-demo)
- [practice/web3-hardhat-demo](./practice/web3-hardhat-demo)

### 重点入口

- [项目介绍与面试话术](./design/project-pitch.md)
- [面试亮点整理](./design/interview-highlights.md)
- [Day13 项目开工总结](./docs/day13-project-kickoff.md)
- [Day21 手动补扫与最小对账第一版](./docs/day21-deposit-rescan-and-account-reconcile.md)
- [Day24 提现回执超时巡检](./docs/day24-withdraw-receipt-timeout-and-exception-governance.md)
- [Day25 nonce 锁与串行广播](./docs/day25-nonce-lock-and-serialized-broadcast.md)
- [Day26 任务幂等、重试上限与失败分层](./docs/day26-task-idempotency-retry-and-failure-tiering.md)
- [Day27 自动对账任务第一版](./docs/day27-automatic-account-reconcile-task.md)
- [Day28 审计日志与监控指标第一版](./docs/day28-audit-log-and-metrics-first-pass.md)
- [Day29 GitHub 展示与面试表达强化](./docs/day29-github-showcase-and-interview-story.md)
- [Day22-Day30 继续推进计划](./roadmap/day22-day30-plan.md)

---

## 学习路线

### Day01-Day03：链上基础认知

- [Day01 账户与钱包](./docs/day01-accounts-and-wallets.md)
- [Day02 交易、Gas、Nonce](./docs/day02-transactions-gas-nonce.md)
- [Day03 EVM、存储与日志](./docs/day03-evm-storage-logs.md)

### Day04-Day08：合约与 Java 读链

- [Day04 Solidity 基础](./docs/day04-solidity-basics.md)
- [Day05 ERC-20、ABI、事件](./docs/day05-erc20-abi-events.md)
- [Day06 Hardhat 工程化流程](./docs/day06-hardhat-workflow.md)
- [Day07 Java + web3j 读链](./docs/day07-java-web3j-rpc.md)
- [Day08 合约调用与日志解码](./docs/day08-contract-calls-and-log-decoding.md)

### Day09-Day12：钱包后端设计

- [Day09 充值监听链路](./docs/day09-deposit-listener.md)
- [Day10 提现链路](./docs/day10-withdrawal-flow.md)
- [Day11 钱包后端架构](./docs/day11-wallet-backend-architecture.md)
- [Day12 最小项目设计](./docs/day12-wallet-backend-project-design.md)

### Day13-Day20：项目落地第一阶段

- [Day13 进入实现阶段前总结](./docs/day13-project-kickoff.md)
- [Day14 实现阶段学习计划](./docs/day14-implementation-study-plan.md)
- [Day15 ERC-20 充值扫描第一版](./docs/day15-erc20-deposit-scan-first-pass.md)
- [Day16 确认数推进与正式入账](./docs/day16-deposit-confirmation-and-credit.md)
- [Day17 提现申请与冻结](./docs/day17-withdraw-apply-and-freeze.md)
- [Day18 提现审核与解冻回退](./docs/day18-withdraw-review-and-unfreeze.md)
- [Day19 提现广播与回执跟踪](./docs/day19-withdraw-broadcast-and-receipt-tracking.md)
- [Day20 提现 nonce 管理与广播重试](./docs/day20-withdraw-nonce-and-retry.md)

### Day21-Day29：项目治理能力增强

- [Day21 手动补扫与最小资产对账](./docs/day21-deposit-rescan-and-account-reconcile.md)
- [Day22 充值链路 reorg 风险识别](./docs/day22-deposit-reorg-detection.md)
- [Day23 充值补偿与冲正](./docs/day23-deposit-compensation-and-reversal.md)
- [Day24 提现回执超时巡检与异常治理](./docs/day24-withdraw-receipt-timeout-and-exception-governance.md)
- [Day25 nonce 锁与串行广播](./docs/day25-nonce-lock-and-serialized-broadcast.md)
- [Day26 任务幂等、重试上限与失败分层](./docs/day26-task-idempotency-retry-and-failure-tiering.md)
- [Day27 自动对账任务第一版](./docs/day27-automatic-account-reconcile-task.md)
- [Day28 审计日志与监控指标第一版](./docs/day28-audit-log-and-metrics-first-pass.md)
- [Day29 GitHub 展示与面试表达强化](./docs/day29-github-showcase-and-interview-story.md)

---

## 当前项目覆盖能力

到 Day29 为止，主项目已经覆盖：

1. ERC-20 `Transfer event` 扫描、候选充值识别、确认数推进、正式入账。
2. 提现申请、余额冻结、审核通过、审核拒绝、链上广播、回执同步。
3. 热钱包 `nonce` 预留、广播失败重试、单 JVM 串行广播保护。
4. 手动补扫、单用户最小资产对账、异常充值 `reorg` 风险识别。
5. 异常充值补偿、冲正流水、`REORG_COMPENSATED` 终态推进。
6. 提现回执超时巡检、`receiptCheckRetryCount` 治理计数、`MANUAL_HANDLE_REQUIRED` 人工处理边界。
7. 提现广播失败治理：
   - `broadcastRetryCount`
   - `RETRYABLE / MANUAL_HANDLE_REQUIRED` 失败分层
   - 最大自动重试上限 `withdrawBroadcastMaxRetryCount`
8. 对外订单查询已可看到治理计数，方便后台排障和项目展示。
9. 自动对账任务、对账结果落库、按用户最近批次 / 按任务批次回查历史快照。
10. 关键后台任务的审计日志、指标概览和批次级可观测性查询。
11. 项目介绍、模块设计、接口清单和面试话术的系统化表达。

---

## 一句话项目介绍

如果你想快速理解这个仓库，可以先记住这句话：

> 这是一个用 Java 后端思路构建的 Web3 资金后端实践项目，重点不在单点链上调用，而在把充值、提现、账本、补偿、对账和可观测性串成完整工程主线。

---

## 当前主项目

`practice/web3-wallet-backend-demo` 是当前最核心的展示项目，技术栈固定为：

- `Java 17`
- `Spring Boot`
- `MyBatis-Plus`
- `MySQL`
- `SpringDoc`
- `Lombok @Data`
- `web3j`

这部分严格按真实后端风格推进：

- 不用内存数据库，直接使用 MySQL
- 实体字段全部保留中文 Javadoc
- 对外接口使用 Javadoc + SpringDoc 零入侵生成文档
- Web3 关键调用补中文注释，强调业务规则和治理边界

---

## 当前进度

- [x] Web3 基础认知与 Java 读链
- [x] 钱包后端项目设计与最小 API 方案
- [x] Spring Boot + MyBatis-Plus + MySQL 工程骨架
- [x] 充值扫描、确认数推进、正式入账
- [x] 提现申请、审核、广播、回执同步
- [x] 手动补扫与单用户最小资产对账
- [x] 充值 reorg 风险识别、补偿与冲正
- [x] 提现回执超时巡检与异常订单治理
- [x] 热钱包 nonce 锁与串行广播
- [x] 任务幂等、重试上限与失败分层第一版
- [x] 自动对账任务第一版
- [x] 审计日志与监控指标第一版
- [x] GitHub 展示与面试表达强化
- [ ] 项目收口与简历表达第一版

---

## 为什么这个仓库适合求职展示

这个仓库比较适合拿去展示，是因为它强调的不是“纯概念”而是“可解释的工程过程”：

1. 每一天都有文档、代码、测试或 README 同步产出。
2. 不是只做 happy path，而是逐步补齐 `reorg`、补偿、超时治理、任务治理。
3. 很多设计点都能直接翻译成面试表述，例如：
   - 幂等
   - 状态机
   - 补偿
   - 对账
   - 审计
   - 定时任务治理

如果你是面试官或者技术负责人，这个仓库最值得看的不是某一个接口，而是下面 3 条线：

1. `充值 -> 提现 -> 账本 -> 流水`
2. `reorg -> 补偿 -> 对账`
3. `任务治理 -> 可观测性 -> 项目表达`

---

## 推荐阅读顺序

如果你是第一次打开这个仓库，推荐这样看：

1. 先看 [Day07](./docs/day07-java-web3j-rpc.md) / [Day09](./docs/day09-deposit-listener.md) / [Day10](./docs/day10-withdrawal-flow.md) / [Day11](./docs/day11-wallet-backend-architecture.md)，快速建立 Web3 后端直觉。
2. 再看 [Day13](./docs/day13-project-kickoff.md) 到 [Day20](./docs/day20-withdraw-nonce-and-retry.md)，理解主项目如何落地。
3. 然后看 [Day21](./docs/day21-deposit-rescan-and-account-reconcile.md) 到 [Day29](./docs/day29-github-showcase-and-interview-story.md)，理解项目是怎么补治理能力和项目表达的。
4. 再看 [design/project-pitch.md](./design/project-pitch.md) 和 [design/interview-highlights.md](./design/interview-highlights.md)，理解这个项目该怎么讲。
5. 最后看 [practice/web3-wallet-backend-demo](./practice/web3-wallet-backend-demo) 和 [roadmap/day22-day30-plan.md](./roadmap/day22-day30-plan.md)，理解当前代码状态和下一步迭代方向。

---

## 目录结构

### `docs/`

按 Day 组织的学习与项目推进文档，当前已覆盖 `Day01-Day29`。

### `practice/`

可运行的最小示例代码：

- `web3-wallet-backend-demo`
- `web3j-demo`
- `web3-hardhat-demo`

### `design/`

更偏系统设计、表结构、接口清单和面试表达。

### `diagrams/`

Mermaid 流程图和架构图。

### `roadmap/`

后续学习计划、项目迭代计划和求职展示节奏。

---

## 下一步

当前最自然的下一步是：

- `Day30：项目收口与简历表达第一版`

重点会放在：

1. 把项目背景、职责、技术栈和关键难点整理成简历友好的表达。
2. 继续收口成可直接复述的项目介绍和项目经历。
3. 把 README、design 文档和简历表达进一步统一起来。

---

## 相关入口

- [主项目 README](./practice/web3-wallet-backend-demo/README.md)
- [Day22-Day30 计划](./roadmap/day22-day30-plan.md)
- [后续任务清单](./roadmap/next-steps.md)
- [项目介绍与面试话术](./design/project-pitch.md)
- [面试亮点整理](./design/interview-highlights.md)
