# web3-hardhat-demo

这个目录用于存放基于 `Hardhat（以太坊开发框架）` 的最小合约工程示例，重点覆盖：

- Solidity 基础语法
- 合约编译、测试与部署
- ERC-20 合约最小实践
- 本地链联调脚本

## 目录说明

- `contracts/Counter.sol`：最小计数器合约
- `contracts/Day8SimpleToken.sol`：Day8 使用的最小 ERC-20 合约
- `scripts/day7-counter-flow.ts`：部署 Counter 并触发一次链上交易
- `scripts/day8-deploy-token.ts`：部署本地 ERC-20 合约
- `scripts/day8-transfer-token.ts`：发起一笔本地 ERC-20 转账
- `test/Counter.ts`：Counter 的 TypeScript 测试
- `contracts/Counter.t.sol`：Counter 的 Solidity 测试

## 常用命令

安装依赖：

```bash
npm install
```

编译合约：

```bash
npx hardhat compile
```

运行测试：

```bash
npx hardhat test
```

启动本地链：

```bash
npx hardhat node
```

执行 Counter 示例：

```bash
npx hardhat run ./scripts/day7-counter-flow.ts --network localhost
```

## 说明

这个目录保留的是当前仓库真正会用到的 Hardhat 示例。  
和当前主题无关的官方模板残留已经清理，避免上传 GitHub 后显得杂乱。

