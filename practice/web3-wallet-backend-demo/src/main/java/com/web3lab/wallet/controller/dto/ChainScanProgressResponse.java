package com.web3lab.wallet.controller.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 链扫描进度响应。
 */
@Data
public class ChainScanProgressResponse {

    /**
     * 扫描进度主键。
     */
    private Long id;

    /**
     * 链编码。
     */
    private String chain;

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 最近扫描到的区块号。
     */
    private Long lastScannedBlock;

    /**
     * 最近更新时间。
     */
    private LocalDateTime updatedAt;

    public ChainScanProgressResponse() {
    }

    public ChainScanProgressResponse(Long id, String chain, String taskName, Long lastScannedBlock,
                                     LocalDateTime updatedAt) {
        this.id = id;
        this.chain = chain;
        this.taskName = taskName;
        this.lastScannedBlock = lastScannedBlock;
        this.updatedAt = updatedAt;
    }
}
