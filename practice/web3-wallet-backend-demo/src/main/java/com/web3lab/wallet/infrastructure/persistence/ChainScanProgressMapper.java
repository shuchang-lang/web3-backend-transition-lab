package com.web3lab.wallet.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web3lab.wallet.domain.scan.ChainScanProgress;

/**
 * 链扫描进度持久化 Mapper。
 */
public interface ChainScanProgressMapper extends BaseMapper<ChainScanProgress> {
}
