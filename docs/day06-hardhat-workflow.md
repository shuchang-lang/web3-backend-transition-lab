今天进入 `Day 6`，主题是 `Hardhat（以太坊开发框架）`。

这一天的目标很明确：\
把你从 `Remix（在线合约练习环境）` 带到“像正式项目一样开发合约”。

**先记一句话**
`Remix` 适合学语法和做小实验，`Hardhat` 适合做工程。

对你这个 8 年 Java 后端来说，`Hardhat` 可以类比成：

*   `Maven/Gradle`：负责编译和任务执行
*   `JUnit`：负责测试
*   本地开发环境：提供 `local network（本地链）`
*   部署工具：把合约部署到测试网或本地链
*   构建产物：生成 `ABI（合约接口描述）` 和 `bytecode（字节码）`

截至 `2026-04-01`，Hardhat 官网首页显示 `Hardhat 3` 已经 production ready，官方也强调它把 `测试、部署、调试、代码覆盖率、验证` 都整合进来了，并且 `Solidity（智能合约语言）` 测试和 `TypeScript` 测试都是一等公民。官方还主推 `Hardhat Ignition（声明式部署工具）` 做部署。[Hardhat 官网](https://hardhat.org/)

***

## 一、为什么 Day 6 一定要学 Hardhat

因为你后面要做的不是“写一个能跑的合约”，而是“写一个能维护、能测试、能部署、能交付的合约项目”。

如果一直停留在 Remix，你会缺这些能力：

*   合约工程目录管理
*   自动编译
*   自动测试
*   本地链调试
*   部署脚本
*   `artifact（构建产物）` 管理
*   团队协作

而这些，恰好就是 Java 后端最熟悉的工程化思维。

***

## 二、Hardhat 里最重要的 4 个动作

今天你不用把插件生态全学完，只抓这 4 个动作：

1.  `init（初始化项目）`
2.  `compile（编译合约）`
3.  `test（跑测试）`
4.  `deploy（部署合约）`

你以后正式做合约开发，日常基本都围绕这 4 件事。

***

## 三、一个 Hardhat 项目大概长什么样

你先记住这个结构：

```text
my-hardhat-project/
  contracts/
    Counter.sol
  test/
    Counter.ts
  ignition/
    modules/
      Counter.ts
  hardhat.config.ts
  package.json
```

每个目录你先这样理解：

*   `contracts/`：放合约源码
*   `test/`：放测试
*   `ignition/modules/`：放部署模块
*   `hardhat.config.ts`：项目配置
*   `package.json`：Node 项目依赖

你可以把它类比成：

*   `contracts/` = `src/main/java`
*   `test/` = `src/test/java`
*   `hardhat.config.ts` = 项目配置文件
*   `ignition/` = 部署脚本/发布流程

***

## 四、Hardhat 到底帮你做了什么

### 1. 编译合约

把 `.sol` 文件编译成：

*   `bytecode（字节码）`
*   `ABI（合约接口描述）`

这两个东西以后都非常重要：

*   `bytecode`：部署合约要用
*   `ABI`：Java 后端用 `web3j（Java 链上交互库）` 调合约、解日志要用

所以你可以把 `Hardhat compile` 理解成：

`把 Solidity 源码变成可部署、可调用的构建产物`

***

### 2. 跑测试

官方现在明确支持两类测试：

*   `Solidity tests（Solidity 测试）`
*   `TypeScript tests（TypeScript 测试）`

你现在作为 Java 后端，不用纠结哪种更“高级”。\
先理解：

*   测试的目标是验证合约逻辑
*   比如 `increment()` 是否真的让 `count + 1`
*   比如 `transfer()` 后余额和事件是否正确

你可以把这一步理解成：

`JUnit 测业务规则，只不过测试对象变成了合约`

***

### 3. 本地链运行

Hardhat 自带本地开发网络，官网也明确把 `Hardhat Network（本地开发链）` 作为核心能力之一。

这意味着你可以：

*   不连接真实测试网
*   在本地反复部署
*   本地打交易
*   本地调试回滚
*   看详细错误栈

这对你非常重要，因为链上开发最怕“每改一处都要上真实网络试”。

***

### 4. 部署合约

Hardhat 3 现在比较强调 `Ignition（声明式部署工具）`。\
你可以先把它理解成：

`把“我要部署哪些合约、按什么顺序部署”写成代码，再让 Hardhat 帮你执行`

这和 Java 里的“脚本化部署”思路很像。

***

## 五、为什么 Java 后端会喜欢 Hardhat

因为它很符合你的工程习惯。

你现在在 Remix 里做的是：

*   打开网页
*   粘代码
*   手动部署
*   手动点函数

而在 Hardhat 里，你开始做的是：

*   项目结构化管理
*   命令行编译
*   自动测试
*   产物输出
*   版本化协作

这其实更像你熟悉的后端工程。

所以从转型角度讲，Day 6 很关键，因为它把你从“学语法”拉到“学工程”。

***

## 六、今天你应该怎么学

### 第一步：理解初始化

当前官方最近的版本说明里明确提到了最小 sample project，可通过 `hardhat --init` 获取。你本机常见会这样用：

```bash
npm init -y
npm install --save-dev hardhat
npx hardhat --init
```

你今天不用死记 CLI 细节，先知道：\
`Hardhat 可以帮你生成最小工程模板`

***

### 第二步：把 Counter 放进 contracts

你已经在 Remix 里写过 `Counter`，现在要开始形成工程意识：

`Counter.sol` 不再只是一个页面里的代码片段，而是项目里的源码文件。

***

### 第三步：编译

你要理解编译后会产出：

*   `ABI`
*   `bytecode`

这一步非常重要，因为 Day 7/8 以后你用 Java 读链、调合约，很多时候就要拿 Hardhat 编出来的 `ABI`。

***

### 第四步：测试

今天先不要求你精通测试语法，你只要知道测试最少会验证这些东西：

*   初始值是不是 0
*   调一次 `increment()` 后是不是变成 1
*   连调两次是不是变成 2

这和你写 Java 单测时的思维完全一样。

***

### 第五步：部署

今天只要先理解：

*   `deploy（部署）` 就是把合约真正发布到某个链环境
*   可以是本地链
*   可以是测试网
*   部署后会得到合约地址

***

## 七、Day 6 最该记住的 6 句话

1.  `Hardhat` 是 Solidity 工程化开发环境。
2.  `Remix` 适合练语法，`Hardhat` 适合做项目。
3.  `compile` 会产出 `ABI` 和 `bytecode`。
4.  `test` 是验证合约逻辑是否符合预期。
5.  `Hardhat Network` 让你可以本地反复调试。
6.  `Ignition` 是 Hardhat 3 重点推荐的部署方式之一。

***

## 八、今天的验收标准

你学完今天，至少要能回答：

1.  为什么不能一直停留在 Remix？
2.  `Hardhat` 和 `Remix` 的核心区别是什么？
3.  `ABI` 和 `bytecode` 是怎么来的？
4.  为什么本地链调试很重要？
5.  为什么合约开发也必须写测试？

***

## 九、你现在先回答这 3 题

1.  为什么 `Hardhat（以太坊开发框架）` 比 `Remix（在线合约练习环境）` 更适合正式项目？
2.  `compile（编译）` 后最重要的两个产物是什么？
3.  `Hardhat Network（本地开发链）` 对开发有什么价值？

如果你答完，我下一条就继续按这个节奏带你进入 `Day 6` 的第二部分：\
`Counter 合约在 Hardhat 里是怎么完成 compile -> test -> deploy 的`。

参考：

*   [Hardhat 官网](https://hardhat.org/)
*   [Hardhat 3 首页](https://hardhat.org/)\
    我主要参考了官网首页关于 `Hardhat 3`、`Hardhat Network`、`Ignition`、`Solidity/TypeScript tests` 和最新版本说明的内容。

