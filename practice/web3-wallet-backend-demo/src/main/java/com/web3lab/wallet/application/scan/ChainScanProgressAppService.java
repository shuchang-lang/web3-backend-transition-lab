package com.web3lab.wallet.application.scan;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.common.exception.ResourceNotFoundException;
import com.web3lab.wallet.config.WalletDefaultsProperties;
import com.web3lab.wallet.controller.dto.ChainScanProgressResponse;
import com.web3lab.wallet.domain.scan.ChainScanProgress;
import com.web3lab.wallet.infrastructure.persistence.ChainScanProgressMapper;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 链扫描进度应用服务，负责维护和查询扫描任务断点。
 */
@Service
public class ChainScanProgressAppService {

    /**
     * ERC-20 充值扫描任务名称。
     */
    public static final String ERC20_DEPOSIT_SCAN_TASK = "erc20_deposit_scan";

    private final ChainScanProgressMapper chainScanProgressMapper;
    private final WalletDefaultsProperties walletDefaultsProperties;

    public ChainScanProgressAppService(ChainScanProgressMapper chainScanProgressMapper,
                                       WalletDefaultsProperties walletDefaultsProperties) {
        this.chainScanProgressMapper = chainScanProgressMapper;
        this.walletDefaultsProperties = walletDefaultsProperties;
    }

    /**
     * 查询指定任务的扫描进度，不存在时自动初始化为 0。
     *
     * @param chain 链编码
     * @param taskName 任务名称
     * @return 扫描进度响应
     */
    @Transactional
    public ChainScanProgressResponse getOrInitProgress(String chain, String taskName) {
        String resolvedChain = resolveOrDefault(chain, walletDefaultsProperties.chain());
        ChainScanProgress progress = findProgress(resolvedChain, taskName);
        if (progress == null) {
            progress = ChainScanProgress.create(resolvedChain, taskName, 0L);
            chainScanProgressMapper.insert(progress);
        }
        return toResponse(progress);
    }

    /**
     * 更新扫描进度断点。
     *
     * @param chain 链编码
     * @param taskName 任务名称
     * @param lastScannedBlock 最近扫描区块
     * @return 扫描进度响应
     */
    @Transactional
    public ChainScanProgressResponse updateProgress(String chain, String taskName, Long lastScannedBlock) {
        String resolvedChain = resolveOrDefault(chain, walletDefaultsProperties.chain());
        ChainScanProgress progress = findProgress(resolvedChain, taskName);
        if (progress == null) {
            throw new ResourceNotFoundException("未找到对应的扫描进度，请先初始化任务");
        }
        progress.setLastScannedBlock(lastScannedBlock);
        chainScanProgressMapper.updateById(progress);
        return toResponse(progress);
    }

    /**
     * 按链和任务查询扫描进度。
     *
     * @param chain 链编码
     * @param taskName 任务名称
     * @return 扫描进度实体，不存在时返回 null
     */
    private ChainScanProgress findProgress(String chain, String taskName) {
        return chainScanProgressMapper.selectOne(Wrappers.<ChainScanProgress>lambdaQuery()
                .eq(ChainScanProgress::getChain, chain)
                .eq(ChainScanProgress::getTaskName, taskName)
                .last("LIMIT 1"));
    }

    /**
     * 在调用方未显式传值时回落到系统默认链配置。
     *
     * @param value 原始输入值
     * @param defaultValue 默认值
     * @return 最终链编码
     */
    private String resolveOrDefault(String value, String defaultValue) {
        return Optional.ofNullable(value)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(defaultValue);
    }

    /**
     * 将扫描进度实体转换为对外响应。
     *
     * @param progress 扫描进度实体
     * @return 扫描进度响应
     */
    private ChainScanProgressResponse toResponse(ChainScanProgress progress) {
        return new ChainScanProgressResponse(
                progress.getId(),
                progress.getChain(),
                progress.getTaskName(),
                progress.getLastScannedBlock(),
                progress.getUpdatedAt()
        );
    }
}
