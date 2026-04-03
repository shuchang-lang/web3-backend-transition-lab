# Day 04 - Solidity Basics

## 学习目标

- 理解 Solidity 合约的最小结构
- 掌握 `state variable（状态变量）`、函数、可见性
- 区分 `view`、`pure`、`payable`
- 理解 `require` 的作用

## 核心概念

### 1. Contract

合约是部署在链上的程序单元。

### 2. State Variable

状态变量最终落在 `storage` 中，是链上正式业务数据。

### 3. 函数分类

- `view`：读状态但不改状态
- `pure`：只计算，不读也不改状态
- `payable`：允许接收原生币

### 4. Require

最常见的前置校验方式，条件不满足时会回滚执行。

## Java 后端映射

- `view` 类似查询接口
- 改状态函数类似写接口
- `payable` 可以理解为“允许携带转账金额的接口”

## 最小实践

- Counter
- Calculator
- SimpleVault

## 阶段结论

Day 4 的重点是：

**先掌握最小合约语法，而不是一开始追求复杂合约设计。**

