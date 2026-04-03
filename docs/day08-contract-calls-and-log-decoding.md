**Day 8**

今天的主题是：`Java 后端如何真正调用合约、理解 ABI（合约接口描述）、解析 event log（事件日志）`

这一天很关键，因为从今天开始，你学的内容会真正靠近 `wallet backend（钱包后端）`、`indexer（索引服务）`、`deposit detection（充值监听）` 的核心工作。

**今天的目标**

1.  搞懂 Java 后端和合约交互需要哪 3 样东西。
2.  分清 `eth_call（只读调用）` 和“发交易”有什么区别。
3.  搞懂日志里的 `topic（日志主题）` 和 `data（日志数据）` 分别装什么。
4.  能解释为什么后端做监听时，核心是在消费“合约地址 + ABI + 日志”。

***

**一、Java 后端怎么和合约打交道**
你先记住一句话：

`Java 后端不是直接“操作合约对象”，而是通过 RPC（节点远程接口） + 合约地址 + ABI 去和链上合约通信。`

要和一个合约交互，最少需要这 3 样：

1.  `RPC URL（节点地址）`
2.  `contract address（合约地址）`
3.  `ABI（合约接口描述）`

可以类比成：

*   `RPC URL` 像服务地址
*   `contract address` 像具体服务实例 ID
*   `ABI` 像接口文档 + 方法签名

少一个都不行。

***

**二、调用合约有两种完全不同的方式**
**1. `eth_call（只读调用）`**

*   不改链上状态
*   不需要真正上链
*   通常不需要你支付 `gas（链上执行消耗）`
*   常用于查余额、查状态、查配置

比如：

*   查 `Counter.x()`
*   查 `ERC-20.balanceOf(address)`

这很像 Java 后端里的“查询接口”。

**2. 发交易**

*   会改链上状态
*   需要签名
*   需要上链
*   需要 `receipt（交易回执）` 才知道最终执行结果

比如：

*   调 `Counter.inc()`
*   调 `ERC-20.transfer()`

这更像“写接口 + 异步确认结果”。

对你这个阶段来说，最该先掌握的是：
`先会读，再学写。`

因为真实 Web3 后端里，大量工作其实是：

*   读链
*   查回执
*   解日志
*   入库
*   对账

不是一开始就疯狂从 Java 发交易。

***

**三、ABI 到底在 Java 后端里起什么作用**
`ABI` 的实际作用主要有两个：

1.  调用函数时，告诉你怎么把参数编码成链能识别的格式
2.  解码事件日志时，告诉你日志里的字段分别是什么

所以 `ABI` 不是“看着好像有用”，而是：

*   没有它，很难正确调合约
*   没有它，很难把日志解成人能看懂的数据

你可以把它理解成：

`合约世界的接口契约`

***

**四、日志为什么比你想象中重要**
后端很多时候并不是“直接查最终结果表”，而是在消费链上执行后的日志。

一个日志通常至少有这些东西：

*   `address（发出日志的合约地址）`
*   `topics（日志主题数组）`
*   `data（日志数据）`

最重要的是：

**1. `topics[0]`**
通常是事件签名哈希，用来标识“这到底是什么事件”。

**2. `topics[1..]`**
通常放 `indexed（可索引）` 参数。

**3. `data`**
放非 `indexed` 参数。

***

**五、用你现在的 Counter 合约理解**
你现在的合约有这个事件：

```solidity
event Increment(uint by);
```

注意，这里的 `by` 没有 `indexed`。

所以它对应的日志特点通常是：

*   `topic0`：`Increment(uint256)` 的事件签名哈希
*   `data`：`by` 的值
*   不会有 `topics[1]` 存 `by`

这点非常重要，因为它让你明白：

`不是所有事件参数都在 topics 里，很多参数其实在 data 里。`

***

**六、再对比 ERC-20 的 Transfer**
`ERC-20` 的标准事件通常是：

```solidity
event Transfer(address indexed from, address indexed to, uint256 value);
```

这时日志一般会长这样：

*   `topic0`：`Transfer` 事件签名
*   `topics[1]`：`from`
*   `topics[2]`：`to`
*   `data`：`value`

所以你之前看到 `Transfer event（转账事件）` 时：

*   转出地址在 `topics`
*   转入地址在 `topics`
*   金额在 `data`

这就是为什么充值监听非常依赖日志结构。

***

**七、为什么这对 Java 后端特别重要**
你以后做充值监听，真实链路通常是：

1.  扫新区块
2.  拿到交易哈希
3.  查 `receipt`
4.  从 `receipt.logs` 里找目标事件
5.  按 `ABI` 解码
6.  拿到 `from / to / amount`
7.  判断 `to` 是否是平台地址
8.  落库，等待 `confirmation（确认数）`

所以对后端来说，重点不是“会不会写一个合约”，而是：

`能不能稳定地从链上执行结果里抽出业务数据`

这就是 Day 8 的核心。

***

**八、你今天最该记住的 6 句话**

1.  Java 后端调用合约，至少需要 `RPC URL + 合约地址 + ABI`。
2.  `eth_call` 是只读调用，不等于真正上链交易。
3.  发交易要等 `receipt` 才知道执行结果。
4.  日志由 `address + topics + data` 组成。
5.  `topic0` 通常标识事件类型。
6.  `indexed` 参数常在 `topics`，非 `indexed` 参数常在 `data`。

***

**九、今天的最小练习**
你先不写新代码，先把概念吃透。今天只做这 3 个练习：

1.  说出 `Java 调合约` 最少需要哪 3 样东西。
2.  说出 `eth_call` 和“发交易”的区别。
3.  说出下面两个事件，哪些字段会在 `topics`，哪些在 `data`：
    *   `Increment(uint by)`
    *   `Transfer(address indexed from, address indexed to, uint256 value)`

***

**十、你先回答这 3 题**

1.  为什么 Java 后端调用合约最少需要 `RPC URL + 合约地址 + ABI`？
2.  `eth_call（只读调用）` 和发交易的本质区别是什么？
3.  `Increment(uint by)` 和 `Transfer(address indexed from, address indexed to, uint256 value)` 的日志结构有什么差别？

参考：
[web3j Quickstart](https://docs.web3j.io/latest/quickstart/)\
[web3j Transactions and Smart Contracts](https://docs.web3j.io/latest/transactions/transactions_and_smart_contracts/)\
[Ethereum Transactions](https://ethereum.org/developers/docs/transactions/)

1.  Java 后端为什么最少需要 `RPC URL（节点地址）`、`contract address（合约地址）`、`ABI（合约接口描述）`\
    因为这三样分别解决三个不同问题：

*   `RPC URL`：告诉你的 Java 程序要连哪一个链节点
*   `contract address`：告诉程序具体要操作哪一个合约实例
*   `ABI`：告诉程序这个合约有哪些函数、参数怎么编码、事件怎么解码

少了 `RPC URL`，你连不上链；\
少了 `contract address`，你不知道调哪个合约；\
少了 `ABI`，你不知道怎么正确构造调用数据，也不知道怎么把日志解出来。

所以这三样本质上就是：

`连接入口 + 目标对象 + 交互契约`

***

1.  `eth_call（只读调用）` 和发交易的本质区别\
    `eth_call` 本质上是“让节点模拟执行一次只读逻辑，然后把结果直接返回给你”，它通常：

*   不改链上状态
*   不需要真正上链
*   不需要等待 `receipt（交易回执）`
*   常用于查余额、查配置、查计数器当前值

而发交易是：

*   真正提交一笔 `transaction（链上交易）`
*   会修改链上状态
*   需要签名
*   要等交易被打包、执行
*   最后通过 `receipt` 看结果

你可以把它类比成：

*   `eth_call`：查询接口
*   发交易：写接口 + 异步结果确认

***

1.  `Increment(uint by)` 和 `Transfer(address indexed from, address indexed to, uint256 value)` 的日志结构差别\
    先记一个规则：

*   `topic0`：通常都是事件签名哈希
*   `indexed` 参数：通常放在 `topics`
*   非 `indexed` 参数：通常放在 `data`

所以：

**`Increment(uint by)`**
因为 `by` 没有 `indexed`，所以通常会是：

*   `topic0`：`Increment(uint256)` 的事件签名
*   `data`：`by` 的值

也就是：\
这个事件主要是“参数在 `data` 里”。

**`Transfer(address indexed from, address indexed to, uint256 value)`**
这里 `from` 和 `to` 都是 `indexed`，`value` 不是，所以通常会是：

*   `topic0`：`Transfer` 事件签名
*   `topics[1]`：`from`
*   `topics[2]`：`to`
*   `data`：`value`

也就是：\
地址在 `topics`，金额在 `data`。

***

**Day 8 总结**
今天你真正要带走的是这 5 个点：

1.  Java 后端接链，本质不是“直接操作合约对象”，而是通过 `RPC + 合约地址 + ABI` 和链交互。
2.  `eth_call` 是只读查询，发交易才会真正改状态。
3.  发交易后，后端真正看的不是“我发出去了没有”，而是 `receipt + event log（事件日志）`。
4.  日志结构里最关键的是：
    *   `topic0` 看事件类型
    *   `topics` 看 `indexed` 参数
    *   `data` 看非 `indexed` 参数
5.  钱包后端、充值监听、索引服务，本质上都是在消费链上执行后的结果。

**一句最适合你记忆的话**
`Web3 后端最核心的能力，不只是会发交易，而是能稳定地从链上的 transaction、receipt、log 里提取业务数据。`

如果你愿意，我下一条可以继续带你进 `Day 9`：\
`如何从 Java 后端视角理解钱包充值监听的完整链路`。
