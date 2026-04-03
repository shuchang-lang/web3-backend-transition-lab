按照 `$web3-java-backend-fast-start` 和 `$web3-java-backend-cn-terms` 的路线，`Day 5` 我们学这 3 个最重要的东西：

*   `ERC-20（同质化代币标准）`
*   `ABI（合约接口描述）`
*   `event log（事件日志）`

这一天非常关键，因为从今天开始，你学的内容会直接连到`钱包后端、充值监听、链上索引、交易所后端`。

**先说今天的目标**
学完后你要能讲清 4 件事：

1.  `ERC-20` 是什么，核心方法有哪些
2.  `ABI` 是什么，为什么后端调用合约离不开它
3.  一笔 token 转账为什么不能只看 `value（原生币金额）`
4.  为什么后端做充值监听，核心盯的是 `Transfer event（转账事件）`

***

## 一、ERC-20 到底是什么

`ERC-20（同质化代币标准）`，你可以把它理解成：

`以太坊上“普通代币”最常见的一套统一接口规范`

“同质化”你先简单理解成：\
每个单位都一样，没有编号差异。\
比如 1 个 USDT 和另 1 个 USDT 没区别。

你可以把 `ERC-20` 类比成：

`Java 里一个所有代币都约定要实现的接口`

也就是说，不同 token 合约虽然项目不一样，但只要它遵守 `ERC-20`，外部系统就能按统一方式去调用它。

***

## 二、ERC-20 最核心的 6 个东西

你先只记这 6 个，已经够后端入门了。

### 1. `totalSupply()`

总代币量

### 2. `balanceOf(address)`

查某个地址余额

### 3. `transfer(to, amount)`

把自己的 token 转给别人

### 4. `approve(spender, amount)`

授权别人最多可以花你多少 token

### 5. `allowance(owner, spender)`

查询授权额度

### 6. `transferFrom(from, to, amount)`

已被授权的一方，代表 `from` 去转 token

你可以先这样理解：

*   `transfer`：自己转
*   `approve + transferFrom`：授权别人代扣

这套组合在 `DEX（去中心化交易所）`、支付、自动扣款里特别常见。

***

## 三、两个最重要的事件

### 1. `Transfer event（转账事件）`

表示有 token 转移发生了

### 2. `Approval event（授权事件）`

表示授权额度发生了变化

为什么事件重要？\
因为后端很多时候不是反复查状态，而是去消费这些事件，做：

*   充值监听
*   转账索引
*   账务入账
*   风险监控

***

## 四、先看一个最小版 ERC-20 思维模型

你现在不用写完整生产级 token，只要先看懂最小逻辑：

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract SimpleToken {
    string public name = "SimpleToken";
    string public symbol = "STK";
    uint8 public decimals = 18;
    uint256 public totalSupply = 1000000 ether;

    mapping(address => uint256) public balanceOf;
    mapping(address => mapping(address => uint256)) public allowance;

    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);

    constructor() {
        balanceOf[msg.sender] = totalSupply;
    }

    function transfer(address to, uint256 amount) public returns (bool) {
        require(balanceOf[msg.sender] >= amount, "insufficient balance");
        balanceOf[msg.sender] -= amount;
        balanceOf[to] += amount;
        emit Transfer(msg.sender, to, amount);
        return true;
    }

    function approve(address spender, uint256 amount) public returns (bool) {
        allowance[msg.sender][spender] = amount;
        emit Approval(msg.sender, spender, amount);
        return true;
    }

    function transferFrom(address from, address to, uint256 amount) public returns (bool) {
        require(balanceOf[from] >= amount, "insufficient balance");
        require(allowance[from][msg.sender] >= amount, "insufficient allowance");

        allowance[from][msg.sender] -= amount;
        balanceOf[from] -= amount;
        balanceOf[to] += amount;

        emit Transfer(from, to, amount);
        return true;
    }
}
```

你今天不用背全部代码，只要看懂这几件事：

*   `balanceOf`：余额表
*   `allowance`：授权额度表
*   `transfer`：直接转账
*   `approve`：设置授权
*   `transferFrom`：代扣转账
*   `emit Transfer(...)`：发出转账事件

***

## 五、ABI 是什么

`ABI（合约接口描述）`，你可以把它理解成：

`后端或前端和合约沟通时用的“方法说明书 + 编解码规则”`

它告诉你：

*   合约有哪些函数
*   每个函数叫什么
*   参数是什么类型
*   返回值是什么类型
*   有哪些事件
*   事件参数长什么样

你可以把 `ABI` 类比成：

*   Java 里的接口定义
*   RPC 的方法签名说明
*   OpenAPI/Swagger 对 HTTP 接口的描述

没有 `ABI`，你就很难正确构造 `calldata（调用参数）`，也很难把日志解码成人类看得懂的数据。

***

## 六、为什么 token 转账不能只看 value

这是 Day 5 最关键的一句话：

`ERC-20 转账通常不是“直接给对方打 ETH”，而是“调用 token 合约的 transfer 方法”。`

这意味着一笔 token 转账常常是这样的：

*   `to` = token 合约地址
*   `value` = 0
*   `data` = 编码后的 `transfer(接收地址, 金额)`
*   执行后产出 `Transfer event`

所以你作为 Java 后端，不能只看：

*   这笔交易给谁了
*   `value` 是多少

你还要看：

*   `data` 里调了什么方法
*   `receipt（交易回执）` 里有哪些日志
*   `Transfer event` 的 `from / to / amount`

***

## 七、Java 后端到底为什么要关心 ABI 和事件

因为你以后做的很多事，本质上都是这个流程：

1.  通过 `RPC（远程调用接口）` 拉交易或区块
2.  用 `ABI` 解码合约调用和事件
3.  从 `Transfer event` 里拿出：
    *   转出地址
    *   转入地址
    *   金额
4.  落库
5.  根据 `confirmation（确认数）` 决定是否入账

这和你熟悉的 Java 后端很像：

*   `ABI` 像接口契约
*   `event log` 像结构化业务事件
*   后端服务像事件消费者

***

## 八、你今天最该记住的 6 句话

1.  `ERC-20` 是以太坊上最常见的普通代币标准。
2.  `transfer` 是自己转，`approve + transferFrom` 是授权代扣。
3.  `Transfer event` 是后端监听 token 转账的核心入口。
4.  `ABI` 是合约方法和事件的描述与编解码规则。
5.  token 转账通常不能只看 `value`。
6.  后端监听充值时，常常要看 `receipt + event log`。

***

## 九、今天建议你做的练习

### 练习 1：先读懂最小 ERC-20

你不用一次背代码，只要能指出：

*   哪一段是余额
*   哪一段是授权
*   哪一段是转账
*   哪一段在发事件

### 练习 2：在 Remix 跑一下

你可以把上面的 `SimpleToken` 合约放到 Remix：

1.  编译
2.  部署
3.  点 `balanceOf(你的部署地址)` 看初始余额
4.  用另一个地址做一次 `transfer`
5.  再看两个地址余额变化

### 练习 3：观察事件

转账成功后，去看交易详情里的 `logs`，观察 `Transfer event`

你今天最重要的体验是：\
`转账成功，不只是余额变了，还多了一条后端可消费的日志`

***

## 十、今天的验收标准

你学完至少要能回答：

1.  `transfer` 和 `transferFrom` 的区别是什么？
2.  为什么 `approve` 存在？
3.  为什么 `ERC-20` 转账不能只看 `value`？
4.  `ABI` 对后端有什么用？
5.  为什么充值监听经常盯 `Transfer event`？

***

我们还是按之前的方式来巩固。你先回答这 3 题：

1.  `ABI（合约接口描述）` 是干什么的？
2.  为什么 `ERC-20（同质化代币标准）` 转账不能只看 `value`？
3.  `Transfer event（转账事件）` 为什么对后端很重要？

