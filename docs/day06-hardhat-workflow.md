# Day 06 - Hardhat Workflow

## 学习目标

- 理解为什么不能一直停留在 `Remix（在线合约 IDE）`
- 跑通 `Hardhat（以太坊开发框架）` 的 `compile`、`test`、`deploy`
- 建立合约工程化开发意识

## 核心概念

### 1. Remix 与 Hardhat 的区别

- Remix：适合快速练习和手动交互
- Hardhat：适合正式工程开发、测试、部署和团队协作

### 2. Compile

把 Solidity 源码编译成：

- `ABI`
- `bytecode（字节码）`

### 3. Test

验证合约状态变化、事件输出和边界条件。

### 4. Deploy

把合约发布到本地链或测试网，并得到合约地址。

## Java 后端映射

- Hardhat 类似 Maven/Gradle + 测试框架 + 部署工具
- 合约开发也需要构建、测试、部署闭环

## 阶段结论

Day 6 的重点是：

**从“在线实验”切换到“工程化开发”。**

