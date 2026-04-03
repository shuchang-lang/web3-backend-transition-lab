今天进入 `Day 4`，主题是 `Solidity（智能合约语言）` 最小语法集。

这一天的目标不是让你变成合约工程师，而是让你先能：

*   看懂一个简单合约
*   写出最小可运行合约
*   理解链上函数和 Java 方法最大的区别
*   为后面看 `ERC-20（同质化代币标准）`、钱包、充值提现打基础

**先记一句话**
`Solidity` 不是“链上的 Java”，它更像“运行在 EVM（以太坊虚拟机）里的状态机代码”。

***

**一、先看一个最小合约长什么样**
先不要纠结每个关键词，先有整体感觉。

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract Counter {
    uint256 public count;

    function increment() public {
        count = count + 1;
    }

    function getCount() public view returns (uint256) {
        return count;
    }
}
```

你可以先这样理解：

*   `contract`：像一个链上的类/服务
*   `count`：状态变量，存在链上
*   `increment()`：改状态的函数
*   `getCount()`：读状态的函数

如果用 Java 类比，它有点像：

```java
public class Counter {
    private long count;

    public void increment() {
        count = count + 1;
    }

    public long getCount() {
        return count;
    }
}
```

但最大的不同是：\
`Solidity` 里的 `count` 真的是写到链上的，改一次就要花 `gas（链上执行消耗）`。

***

**二、状态变量是什么**
`state variable（状态变量）` 就是合约长期保存的数据，最终落在 `storage（合约持久化存储）` 里。

比如：

```solidity
uint256 public count;
address public owner;
mapping(address => uint256) public balances;
```

你可以把它类比成：

*   Java 对象字段 + 数据库持久化数据的结合体

和普通 Java 变量不同的是：

*   它不是请求结束就没了
*   它是链上正式状态
*   每次修改它都更贵

你现在先记住：
`状态变量 = 合约的正式业务数据`

***

**三、函数怎么理解**
`function（函数）` 就是合约暴露出来的可调用逻辑。

最简单分两类：

1.  读函数
2.  写函数

例子：

```solidity
function getCount() public view returns (uint256) {
    return count;
}

function increment() public {
    count = count + 1;
}
```

区别是：

*   `getCount()` 只读，不改状态
*   `increment()` 改状态

对 Java 后端来说很好理解：

*   `view` 函数像查询接口
*   普通写函数像更新接口

但链上要多记一点：\
`改状态的函数一般需要交易和 gas，只读函数通常可以直接查，不一定发交易`

***

**四、可见性是什么**
这是面试高频点，但不难。

`visibility（可见性）` 常见 4 个：

1.  `public`\
    谁都能调，合约内外都能访问

2.  `private`\
    只能在当前合约内部访问

3.  `internal`\
    当前合约和子合约可以访问

4.  `external`\
    主要给合约外部调用，合约内部直接调它不如 `public` 顺手

你先按 Java 类比记：

*   `public`：公开方法
*   `private`：私有方法/字段
*   `internal`：有点像“对子类开放”
*   `external`：更像“专门给外部入口用的方法”

最常见的是先用：

*   `public`
*   `internal`
*   少量 `private`
*   面向外部接口时用 `external`

***

**五、view / pure / payable 是什么**
这 3 个非常重要。

**1. `view（只读函数）`**
表示这个函数会读取状态，但不会修改状态。

```solidity
function getCount() public view returns (uint256) {
    return count;
}
```

类比：
像查询接口，读数据库但不更新。

***

**2. `pure（纯计算函数）`**
表示这个函数既不读状态，也不改状态，只做纯计算。

```solidity
function add(uint256 a, uint256 b) public pure returns (uint256) {
    return a + b;
}
```

类比：
像一个纯工具方法。

***

**3. `payable（可接收原生币）`**
表示这个函数允许随调用一起接收 `ETH（以太坊原生币）`。

```solidity
function deposit() public payable {
}
```

如果没有 `payable`，你往函数里带 ETH，调用就会失败。

你先记最简单版：

*   `view`：读状态
*   `pure`：只计算
*   `payable`：能收钱

***

**六、require / revert 是什么**
它们就是链上的校验和失败中断。

**1. `require（条件校验）`**
最常用，用来检查前置条件。

```solidity
function withdraw(uint256 amount) public {
    require(amount > 0, "amount must be > 0");
    require(count >= amount, "insufficient balance");
}
```

你可以类比成 Java 里的：

```java
if (amount <= 0) {
    throw new IllegalArgumentException("amount must be > 0");
}
```

***

**2. `revert（回滚执行）`**
表示终止执行并回滚状态。

```solidity
if (amount == 0) {
    revert("amount is zero");
}
```

在使用体验上，你现在可以先把它理解成：

*   `require`：更常见的条件断言
*   `revert`：显式回滚

不需要一开始抠太细，先能用就行。

***

**七、一个稍微完整一点的例子**
看这个合约，把今天内容串起来：

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract SimpleVault {
    address public owner;
    uint256 public totalDeposits;

    constructor() {
        owner = msg.sender;
    }

    function deposit() public payable {
        require(msg.value > 0, "must send ETH");
        totalDeposits = totalDeposits + msg.value;
    }

    function getTotalDeposits() public view returns (uint256) {
        return totalDeposits;
    }

    function add(uint256 a, uint256 b) public pure returns (uint256) {
        return a + b;
    }
}
```

你现在可以这样读：

*   `owner`、`totalDeposits` 是状态变量
*   `constructor` 是部署时执行一次的初始化逻辑
*   `deposit()` 是 `payable`，说明能收 ETH
*   `require` 做参数校验
*   `getTotalDeposits()` 是 `view`
*   `add()` 是 `pure`

***

**八、Java 后端最容易误解的地方**

1.  不是所有函数调用都等于“发交易”\
    `view`/`pure` 读调用很多时候只是 RPC 查询，不一定上链。

2.  改状态函数的成本更高\
    因为会真的写链上状态，还要消耗 `gas`。

3.  合约函数不是你想怎么改就怎么改\
    一旦部署，升级成本高得多，不像 Java 服务发版那么自由。

4.  `public` 状态变量会自动生成 getter\
    这点和 Java 不一样。

***

**九、今天你必须记住的 6 句话**

1.  `contract` 是链上的程序单元。
2.  `state variable` 是合约的长期状态。
3.  `view` 读状态但不改状态。
4.  `pure` 只计算，不读也不改状态。
5.  `payable` 表示函数可以接收原生币。
6.  `require` 是最常见的前置校验方式。

***

**十、今天建议你做的 3 个小练习**

1.  自己写一个 `Counter`
    要求：

*   有 `count`
*   有 `increment()`
*   有 `getCount()`

1.  再写一个 `Calculator`
    要求：

*   写一个 `add()` 函数
*   用 `pure`

1.  再写一个 `SimpleVault`
    要求：

*   有 `deposit()`
*   用 `payable`
*   用 `require(msg.value > 0, "...")`

***

**十一、今天的验收标准**
你学完后，至少要能回答：

1.  `view` 和普通写函数的区别是什么？
2.  `pure` 和 `view` 的区别是什么？
3.  为什么 `deposit()` 要加 `payable`？
4.  为什么 `require` 很常见？
5.  状态变量为什么比普通临时变量更重要？

现在我们按之前的方式来巩固。你直接回答这 3 题：

1.  `view（只读函数）` 和普通写函数有什么区别？
2.  `payable（可接收原生币）` 是干什么的？
3.  `require（条件校验）` 在 Solidity 里大概相当于 Java 里的什么？

