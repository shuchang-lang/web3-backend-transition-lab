按照 $web3-java-backend-fast-start 的路线，Day 7 我们开始把前面学的链上概念真正接回到 Java。
今天的主题是：
- web3j（Java 链上交互库）
- RPC（节点远程接口）
- transaction receipt（交易回执）
- event log（事件日志）
  今天的目标
  你学完后要能理解这 4 件事：
  1.Java 后端不是“直接连区块链数据库”，而是通过 RPC 调节点
  2.web3j 本质上是对 JSON-RPC（节点远程协议） 的 Java 封装
  3.后端读链最常见的 4 类数据：block（区块）、transaction（交易）、receipt（回执）、log（日志）
  4.为什么以后做充值监听、索引服务，核心是“查回执和查日志”
--------------------------------------------------------------------------------
1. 先建立 Java 视角的整体图
   你可以把 web3j 理解成：
   Java 版的链上客户端 SDK
   它帮你把这些底层 RPC 方法封装成了 Java 调用，比如：
- eth_blockNumber：查最新区块高度
- eth_getBalance：查地址余额
- eth_getTransactionReceipt：查交易执行结果
- eth_getLogs：查事件日志
- eth_call：只读调用合约
  也就是说，Java 后端以后做的很多事情，本质上就是：
  1.调 RPC
  2.拿到链上结果
  3.解析成业务数据
  4.落库 / 入账 / 通知 / 审计
--------------------------------------------------------------------------------
2. Java 接链第一步：先连上节点
   最小代码先看这个：
   import org.web3j.protocol.Web3j;
   import org.web3j.protocol.http.HttpService;

Web3j web3j = Web3j.build(new HttpService("http://127.0.0.1:8545"));

这句的意思就是：
- 通过 HttpService 连到一个链节点
- 然后用 web3j 发各种链上请求
  你可以把它类比成：
- Java 里初始化一个 RPC client
- 或者初始化一个外部服务 SDK
  注意一个现实点
  你前面用 Hardhat Ignition 部署 Counter 时，终端提示过“结果会丢失”，因为你当时用的是进程内临时链。
  所以如果 Day 7 想让 Java 真正连上你自己的本地链，应该让 npx.cmd hardhat node 常驻运行，再让 Java 连 127.0.0.1:8545。
--------------------------------------------------------------------------------
3. 最常见的第一个请求：查最新区块
   import java.math.BigInteger;

BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
System.out.println("latest block = " + blockNumber);

这一步的意义很简单：
- 验证你的 Java 程序已经真的连上节点了
- 也是最常见的健康检查
--------------------------------------------------------------------------------
4. 第二个请求：查地址余额
   import org.web3j.protocol.core.DefaultBlockParameterName;

String address = "0x...";
BigInteger balance = web3j
.ethGetBalance(address, DefaultBlockParameterName.LATEST)
.send()
.getBalance();

System.out.println("balance = " + balance);

这里查到的是原生币余额，比如 ETH（以太坊原生币）。
这一步和 Day 5 的 ERC-20 余额不一样，ERC-20 余额后面一般要通过合约调用 balanceOf 来读。
--------------------------------------------------------------------------------
5. 第三个请求：查交易和回执
   这一步是 Day 7 最关键的重点之一。
   查交易回执
   import java.util.Optional;
   import org.web3j.protocol.core.methods.response.TransactionReceipt;

String txHash = "0x...";
Optional<TransactionReceipt> receiptOpt = web3j
.ethGetTransactionReceipt(txHash)
.send()
.getTransactionReceipt();

if (receiptOpt.isPresent()) {
TransactionReceipt receipt = receiptOpt.get();
System.out.println("status = " + receipt.getStatus());
System.out.println("gas used = " + receipt.getGasUsed());
System.out.println("logs size = " + receipt.getLogs().size());
} else {
System.out.println("receipt not ready yet");
}

你要记住：
- transaction 更像“请求”
- receipt 更像“执行结果单”
  后端很多时候不是只看交易哈希，而是要等 receipt 出来，才能知道：
- 成功还是失败
- 消耗多少 gas
- 发了哪些日志
--------------------------------------------------------------------------------
6. 第四个请求：查事件日志
   这是你以后做 wallet backend（钱包后端）、indexer（索引服务）、deposit detection（充值监听） 的核心能力。
   先查某个合约的所有日志
   import org.web3j.protocol.core.DefaultBlockParameter;
   import org.web3j.protocol.core.methods.request.EthFilter;
   import org.web3j.protocol.core.methods.response.EthLog;

String contractAddress = "0x...";
EthFilter filter = new EthFilter(
DefaultBlockParameter.valueOf(BigInteger.ZERO),
DefaultBlockParameterName.LATEST,
contractAddress
);

EthLog ethLog = web3j.ethGetLogs(filter).send();
System.out.println("logs count = " + ethLog.getLogs().size());

如果只想查特定事件
你当前的 Counter 合约会发 Increment(uint by) 事件，所以后面可以按事件签名过滤。
你先把思路记住就行：
- 先确定合约地址
- 再确定事件签名
- 再查日志
- 最后解码参数
--------------------------------------------------------------------------------
7. Day 7 最重要的心智模型
   你现在可以把后端读链分成这 4 层：
   1.block
   看链推进到哪了
   1.transaction
   看用户提交了什么请求
   1.receipt
   看请求执行结果
   1.log
   看执行后产生了哪些结构化事件
   以后做 Web3 后端，最常见的真实链路就是：
   扫区块 -> 找交易 -> 查回执 -> 解日志 -> 落库
--------------------------------------------------------------------------------
8. ABI（合约接口描述） 在 Day 7 的作用
   到了 Day 7，ABI 就不再只是概念了，它开始有实际用途：
- 调合约函数时，要靠 ABI 知道参数类型
- 解 event log 时，要靠 ABI 知道事件字段结构
- 生成 Java wrapper 时，也要靠 ABI
  但按照“快速入门”的路线，我建议你今天先不要急着学 wrapper 生成。
  先把“原始读链”思路吃透，更有利于你以后理解钱包、充值监听、索引服务。
--------------------------------------------------------------------------------
9. 今天你最该记住的 6 句话
   1.web3j 是 Java 对链节点 RPC 的封装。
   2.Java 后端读链，核心不是“查数据库”，而是“调节点”。
   3.transaction 是请求，receipt 是执行结果。
   4.receipt 里通常会带 log。
   5.做监听和索引，核心是查 receipt + log。
   6.ABI 是合约调用和日志解码的契约说明。
--------------------------------------------------------------------------------
10. 今天建议你做的最小练习
    按这个顺序来：
    1.连接一个节点，先打出最新区块高度
    2.查一个地址的原生币余额
    3.查一笔交易的回执
    4.打印回执里的日志条数
    如果你想和你当前 Hardhat 工程连起来，最实用的下一步是：
- 一个终端跑 npx.cmd hardhat node
- 另一个终端把 Counter 部署到可持续的本地链
- Java 程序连 http://127.0.0.1:8545
- 先做“查 block / 查 balance / 查 receipt”
--------------------------------------------------------------------------------
11. 今天先做这 3 道题
    你先不用写完整代码，先用自己的话回答：
    1.为什么 Java 后端接链，本质上是调 RPC（节点远程接口）？
    2.transaction receipt（交易回执） 和 transaction（交易） 有什么区别？
    3.为什么说后端很多时候真正关心的是 receipt + event log（事件日志）？
    参考：
- web3j Quickstart
- web3j Transactions and Smart Contracts
- Ethereum Transactions