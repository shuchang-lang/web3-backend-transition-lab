package com.web3lab.wallet.domain.address;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 充值地址实体，对应平台为用户分配的链上入金地址。
 */
@Data
@TableName("wallet_address")
public class WalletAddress {

    /**
     * 地址记录主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 平台用户 ID。
     */
    private Long userId;

    /**
     * 链编码，例如 ETH_SEPOLIA。
     */
    private String chain;

    /**
     * 币种符号，例如 USDT。
     */
    private String tokenSymbol;

    /**
     * 平台分配的充值地址。
     */
    private String address;

    /**
     * 地址状态，当前骨架阶段固定为 ACTIVE。
     */
    private String status;

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
     * 创建充值地址实体。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码
     * @param tokenSymbol 币种符号
     * @param address 平台分配的充值地址
     * @param status 地址状态
     * @return 充值地址实体
     */
    public static WalletAddress create(Long userId, String chain, String tokenSymbol, String address, String status) {
        WalletAddress walletAddress = new WalletAddress();
        walletAddress.setUserId(userId);
        walletAddress.setChain(chain);
        walletAddress.setTokenSymbol(tokenSymbol);
        walletAddress.setAddress(address);
        walletAddress.setStatus(status);
        return walletAddress;
    }
}
