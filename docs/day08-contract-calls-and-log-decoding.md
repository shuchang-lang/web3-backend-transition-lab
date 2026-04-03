# Day 08 - Contract Calls And Log Decoding

## 学习目标

- 理解 Java 后端调用合约最少需要哪三样东西
- 区分 `eth_call（只读调用）` 和发交易
- 理解日志里的 `topics` 和 `data` 结构

## 核心概念

### 1. 调合约的三要素

- `RPC URL`
- `contract address（合约地址）`
- `ABI`

### 2. Eth Call

只读调用，不修改链上状态，也不需要等交易确认。

### 3. Topics 与 Data

- `topic0` 通常标识事件类型
- `indexed` 参数通常在 `topics`
- 非 `indexed` 参数通常在 `data`

## Java 后端映射

- eth_call 类似查询接口
- 发交易类似写接口，但结果是异步确认

## 阶段结论

Day 8 的重点是：

**后端真正关心的往往不是“调没调函数”，而是“执行后留下了什么结果”。**

