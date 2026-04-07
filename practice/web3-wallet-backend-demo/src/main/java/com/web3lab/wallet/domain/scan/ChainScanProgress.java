package com.web3lab.wallet.domain.scan;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 链扫描进度实体，用于支持区块扫描断点续跑。
 */
@Data
@TableName("chain_scan_progress")
public class ChainScanProgress {

    /**
     * 扫描进度主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 链编码，例如 ETH_SEPOLIA。
     */
    private String chain;

    /**
     * 任务名称，例如 erc20_deposit_scan。
     */
    private String taskName;

    /**
     * 最近一次扫描完成的区块号。
     */
    private Long lastScannedBlock;

    /**
     * 记录创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录更新时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建扫描进度记录。
     *
     * @param chain 链编码
     * @param taskName 任务名称
     * @param lastScannedBlock 最近扫描区块
     * @return 扫描进度实体
     */
    public static ChainScanProgress create(String chain, String taskName, Long lastScannedBlock) {
        ChainScanProgress progress = new ChainScanProgress();
        progress.setChain(chain);
        progress.setTaskName(taskName);
        progress.setLastScannedBlock(lastScannedBlock);
        return progress;
    }
}
