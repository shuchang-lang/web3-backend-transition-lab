package com.web3lab.wallet.application.deposit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.web3lab.wallet.domain.address.WalletAddress;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import com.web3lab.wallet.infrastructure.persistence.WalletAddressMapper;
import com.web3lab.wallet.infrastructure.web3.Erc20TransferLog;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 候选充值识别应用服务。
 *
 * <p>它负责把链上读取到的 ERC-20 Transfer 日志和平台地址表做匹配，
 * 识别出“打到平台地址”的候选充值，并以幂等方式写入 deposit_record。</p>
 */
@Service
public class DepositCandidateAppService {

    private final WalletAddressMapper walletAddressMapper;
    private final DepositRecordMapper depositRecordMapper;

    public DepositCandidateAppService(WalletAddressMapper walletAddressMapper,
                                      DepositRecordMapper depositRecordMapper) {
        this.walletAddressMapper = walletAddressMapper;
        this.depositRecordMapper = depositRecordMapper;
    }

    /**
     * 识别并写入候选充值记录。
     *
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @param tokenDecimals 代币精度
     * @param transferLogs 本轮扫描得到的 Transfer 日志
     * @return 本轮新写入的候选充值数量
     */
    public int detectAndStore(String chain, String tokenSymbol, int tokenDecimals, List<Erc20TransferLog> transferLogs) {
        if (CollectionUtils.isEmpty(transferLogs)) {
            return 0;
        }

        List<String> toAddresses = transferLogs.stream()
                .map(Erc20TransferLog::getToAddress)
                .filter(StringUtils::hasText)
                .map(this::normalizeAddress)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(toAddresses)) {
            return 0;
        }

        List<WalletAddress> walletAddresses = walletAddressMapper.selectList(
                Wrappers.<WalletAddress>lambdaQuery()
                        .eq(WalletAddress::getChain, chain)
                        .eq(WalletAddress::getTokenSymbol, tokenSymbol)
                        .in(WalletAddress::getAddress, toAddresses)
        );
        Map<String, WalletAddress> walletAddressIndex = walletAddresses.stream()
                .collect(Collectors.toMap(
                        walletAddress -> normalizeAddress(walletAddress.getAddress()),
                        Function.identity(),
                        (left, right) -> left
                ));
        if (walletAddressIndex.isEmpty()) {
            return 0;
        }

        int insertedCount = 0;
        for (Erc20TransferLog transferLog : transferLogs) {
            WalletAddress walletAddress = walletAddressIndex.get(normalizeAddress(transferLog.getToAddress()));
            if (walletAddress == null) {
                continue;
            }

            DepositRecord depositRecord = DepositRecord.createPending(
                    walletAddress.getUserId(),
                    chain,
                    tokenSymbol,
                    normalizeAddress(transferLog.getContractAddress()),
                    normalizeAddress(transferLog.getFromAddress()),
                    normalizeAddress(transferLog.getToAddress()),
                    transferLog.getTxHash(),
                    transferLog.getLogIndex(),
                    convertAmount(transferLog, tokenDecimals),
                    transferLog.getBlockNumber()
            );
            try {
                // 允许重复扫描同一窗口，真正的幂等兜底交给唯一键 (tx_hash, log_index)。
                depositRecordMapper.insert(depositRecord);
                insertedCount++;
            } catch (DuplicateKeyException exception) {
                // 重复记录说明该日志此前已经识别过，当前轮次直接跳过即可。
            }
        }
        return insertedCount;
    }

    /**
     * 把链上最小单位金额转换为数据库中更易展示的十进制金额。
     *
     * @param transferLog 标准化 Transfer 日志
     * @param tokenDecimals 代币小数位
     * @return 十进制金额
     */
    private BigDecimal convertAmount(Erc20TransferLog transferLog, int tokenDecimals) {
        return new BigDecimal(transferLog.getRawAmount()).movePointLeft(tokenDecimals);
    }

    /**
     * 统一地址大小写与 0x 前缀，避免地址匹配时因展示格式不同而丢单。
     *
     * @param address 原始地址
     * @return 标准化地址
     */
    private String normalizeAddress(String address) {
        if (!StringUtils.hasText(address)) {
            return address;
        }
        String cleanHex = address.startsWith("0x") || address.startsWith("0X") ? address.substring(2) : address;
        return "0x" + cleanHex.toLowerCase(Locale.ROOT);
    }
}
