package com.web3lab.wallet.controller.dto;

import lombok.Data;

/**
 * 手动补扫充值任务响应。
 */
@Data
public class DepositRescanResponse {

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 请求中的起始区块。
     */
    private Long fromBlock;

    /**
     * 请求中的结束区块。
     */
    private Long toBlock;

    /**
     * 本次实际补扫到的结束区块。
     */
    private Long effectiveToBlock;

    /**
     * 当前链头区块。
     */
    private Long latestBlock;

    /**
     * 本轮新识别到的候选充值数量。
     */
    private Integer detectedCount;

    /**
     * 本轮推进完成的正式入账数量。
     */
    private Integer creditedCount;

    /**
     * 本轮是否真正执行了链上补扫。
     */
    private Boolean executed;

    /**
     * 任务结果说明。
     */
    private String remark;

    public DepositRescanResponse() {
    }

    public DepositRescanResponse(String taskName, Long fromBlock, Long toBlock, Long effectiveToBlock, Long latestBlock,
                                 Integer detectedCount, Integer creditedCount, Boolean executed, String remark) {
        this.taskName = taskName;
        this.fromBlock = fromBlock;
        this.toBlock = toBlock;
        this.effectiveToBlock = effectiveToBlock;
        this.latestBlock = latestBlock;
        this.detectedCount = detectedCount;
        this.creditedCount = creditedCount;
        this.executed = executed;
        this.remark = remark;
    }
}
