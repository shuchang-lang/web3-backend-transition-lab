# Current Status

## 当前进度

- 当前已推进到：`Day30`
- 当前阶段主题：`项目收口与简历表达第一版`

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
- Day30：项目收口与简历表达第一版

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
  - 简历项目经历材料与岗位定制表达

## Day30 本次落地

- 新增简历项目经历材料：`design/resume-project-story.md`
- 同步项目表达文档：
  - `design/project-pitch.md`
  - `design/interview-highlights.md`
- 新增 Day30 文档：`docs/day30-project-wrap-up-and-resume-story.md`
- 根 README 已同步到 Day30：
  - 项目当前进度
  - 简历材料入口
  - 下一阶段方向
- 主项目 README 已同步到 Day30：
  - 当前项目阶段
  - 简历写法入口
  - 下一阶段方向
- roadmap 已同步：
  - `roadmap/day22-day30-plan.md`
  - `roadmap/next-steps.md`

## 下一步

- 进入：`第三阶段：让项目更像真实资金后端`
- 优先主题：`更完整的任务治理表达与多实例广播锁升级方案`
- 目标：
  - 把当前单 JVM 维度的任务治理继续补成更接近生产的话语体系
  - 为多实例场景下的广播锁升级留出更明确的设计方案
  - 继续完善监控、告警、费率、风控与运维治理说明
