# Day 05 - ERC20 ABI Events

## 学习目标

- 理解 `ERC-20（同质化代币标准）` 的核心接口
- 理解 `ABI（合约接口描述）` 的实际作用
- 理解 `Transfer event（转账事件）` 为什么是充值监听核心入口

## 核心概念

### 1. ERC-20

以太坊上最常见的普通代币标准，常见接口包括：

- `balanceOf`
- `transfer`
- `approve`
- `allowance`
- `transferFrom`

### 2. ABI

ABI 是合约方法和事件的描述与编解码规则。没有 ABI，很难正确调用合约，也很难把日志解码成人能读懂的数据。

### 3. Transfer Event

ERC-20 转账执行成功后，通常会发出 `Transfer` 事件，包含：

- `from`
- `to`
- `value`

## Java 后端映射

- ABI 很像接口契约或 RPC 方法签名说明
- Transfer event 很像结构化资金流水事件

## 常见误区

- 只看交易的 value，不看 calldata 和日志
- 把 ERC-20 转账等同于原生币转账

## 阶段结论

Day 5 的重点是：

**Web3 后端不能只看交易表面字段，更要看执行结果和事件日志。**

