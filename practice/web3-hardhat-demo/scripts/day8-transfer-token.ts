import { network } from "hardhat";

/**
 * 本地演示默认转 5 枚 token。
 * 这里仍然使用最小单位，保证和 Java 落库字段 amount_raw 的口径一致。
 */
const DEFAULT_TRANSFER_AMOUNT = 5n * 10n ** 18n;

/**
 * 从环境变量读取必填参数。
 * 联调脚本需要显式传 token 地址和充值地址，避免误把旧部署地址拿来复用。
 */
function requireEnv(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) {
    throw new Error(`缺少环境变量 ${name}`);
  }
  return value;
}

async function main() {
  const targetNetwork = process.env.DAY8_NETWORK?.trim() || "localhost";
  const tokenAddress = requireEnv("DAY8_TOKEN_ADDRESS");
  const toAddress = requireEnv("DAY8_TO_ADDRESS");
  const transferAmount = BigInt(process.env.DAY8_TRANSFER_AMOUNT?.trim() || DEFAULT_TRANSFER_AMOUNT.toString());
  const senderIndex = Number(process.env.DAY8_WALLET_INDEX?.trim() || "0");

  const { viem } = await network.connect({ network: targetNetwork });
  const publicClient = await viem.getPublicClient();
  const walletClients = await viem.getWalletClients();
  const senderWalletClient = walletClients.at(senderIndex);

  if (!senderWalletClient) {
    throw new Error(`未找到索引为 ${senderIndex} 的本地测试账户`);
  }

  const token = await viem.getContractAt("Day8SimpleToken", tokenAddress, {
    client: {
      public: publicClient,
      wallet: senderWalletClient,
    },
  });

  const beforeBalance = await token.read.balanceOf([toAddress]);

  console.log("发起 Day8SimpleToken 转账...");
  console.log("network:", targetNetwork);
  console.log("from:", senderWalletClient.account.address);
  console.log("to:", toAddress);
  console.log("amount:", transferAmount.toString());

  const txHash = await token.write.transfer([toAddress, transferAmount]);
  const receipt = await publicClient.waitForTransactionReceipt({ hash: txHash });
  const afterBalance = await token.read.balanceOf([toAddress]);

  console.log("txHash:", txHash);
  console.log("receiptStatus:", receipt.status);
  console.log("logsCount:", receipt.logs.length);
  console.log("balanceBefore:", beforeBalance.toString());
  console.log("balanceAfter:", afterBalance.toString());
}

main().catch((error) => {
  console.error("执行 Day8SimpleToken 转账失败");
  console.error(error);
  process.exitCode = 1;
});
