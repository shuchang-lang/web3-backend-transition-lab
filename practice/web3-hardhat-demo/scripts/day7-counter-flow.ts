import { network } from "hardhat";

async function main() {
  const { viem } = await network.connect();

  console.log("Deploying Counter...");
  const counter = await viem.deployContract("Counter");
  console.log("Counter deployed at:", counter.address);

  const before = await counter.read.x();
  console.log("Counter value before inc():", before.toString());

  const txHash = await counter.write.inc();
  console.log("inc() tx hash:", txHash);

  const receipt = await (await viem.getPublicClient()).getTransactionReceipt({ hash: txHash });
  console.log("receipt status:", receipt.status);
  console.log("logs count:", receipt.logs.length);

  const after = await counter.read.x();
  console.log("Counter value after inc():", after.toString());
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
