import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";

import { network } from "hardhat";

/**
 * Day8 本地联调默认铸造 100 万枚 token。
 * 这里直接按 18 位精度换算成最小单位，方便后续脚本继续沿用同一套口径。
 */
const DEFAULT_INITIAL_SUPPLY = 1_000_000n * 10n ** 18n;

/**
 * 统一解析部署结果输出文件路径。
 * 手动联调时可以通过环境变量指定输出位置，便于后续读取 token 地址和元信息。
 */
function resolveOutputFile(): string {
  const outputFile = process.env.DAY8_TOKEN_OUTPUT_FILE?.trim();
  if (outputFile) {
    return resolve(outputFile);
  }

  return resolve("artifacts/day8/day8-token.json");
}

async function main() {
  const targetNetwork = process.env.DAY8_NETWORK?.trim() || "localhost";
  const { viem } = await network.connect({ network: targetNetwork });
  const [defaultWalletClient] = await viem.getWalletClients();

  if (!defaultWalletClient) {
    throw new Error("未获取到可用的本地测试账户");
  }

  const initialHolder = process.env.DAY8_INITIAL_HOLDER?.trim() || defaultWalletClient.account.address;
  const initialSupply = BigInt(process.env.DAY8_INITIAL_SUPPLY?.trim() || DEFAULT_INITIAL_SUPPLY.toString());
  const outputFile = resolveOutputFile();

  console.log("部署 Day8SimpleToken...");
  console.log("network:", targetNetwork);
  console.log("initialHolder:", initialHolder);
  console.log("initialSupply:", initialSupply.toString());

  const token = await viem.deployContract("Day8SimpleToken", [initialHolder, initialSupply]);
  const initialBalance = await token.read.balanceOf([initialHolder]);

  mkdirSync(dirname(outputFile), { recursive: true });
  writeFileSync(
    outputFile,
    JSON.stringify(
      {
        tokenAddress: token.address,
        tokenSymbol: "D8T",
        tokenDecimals: 18,
        initialHolder,
        initialSupply: initialSupply.toString(),
        initialHolderBalance: initialBalance.toString(),
      },
      null,
      2,
    ),
  );

  console.log("tokenAddress:", token.address);
  console.log("initialHolderBalance:", initialBalance.toString());
  console.log("metadataFile:", outputFile);
}

main().catch((error) => {
  console.error("部署 Day8SimpleToken 失败");
  console.error(error);
  process.exitCode = 1;
});
