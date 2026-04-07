# Current Status

## 当前进度

- 当前已推进到：`Day29`
- 当前阶段主题：`GitHub 展示与面试表达强化`

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
- Day28：审计日志与监控指标第一版
- Day29：GitHub 展示与面试表达强化

## 当前主工程状态

- 工程路径：`practice/web3-wallet-backend-demo`
- 当前测试结果：`mvn test` 通过
- 当前测试数量：`62`

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
- 任务可观测性：
  - `task_audit_log` 审计日志落库
  - 关键任务独立事务留痕
  - 最近审计日志查询
  - 核心任务指标概览查询
- 项目表达材料：
  - 30 秒 / 90 秒 / 3 分钟项目介绍
  - GitHub 展示阅读路径
  - 面试亮点与高频问答
  - 模块设计与接口清单收口

## Day29 本次落地

- 新增项目介绍文档：`design/project-pitch.md`
- 重写面试亮点文档：`design/interview-highlights.md`
- 重写模块设计文档：`design/wallet-backend-module-design.md`
- 重写接口清单文档：`design/wallet-backend-api-list.md`
- 新增 Day29 文档：`docs/day29-github-showcase-and-interview-story.md`
- 根 README 已补强：
  - 一句话项目介绍
  - 更清晰的阅读顺序
  - GitHub / 面试入口
- 主项目 README 已补强：
  - 一句话怎么讲
  - 最适合讲的 5 个亮点
  - 建议演示顺序

## 下一步

- 进入：`Day30`
- 主题：`项目收口与简历表达第一版`
- 目标：
  - 把项目背景、职责、技术栈和关键难点整理成简历友好的表达
  - 形成更完整的项目介绍与可直接复述的项目经历
  - 让 README、design 文档和简历表述进一步统一
