package com.web3lab.wallet.infrastructure.web3;

import lombok.Data;

/**
 * 充值日志链上核验结果。
 *
 * <p>Day22 开始引入最小 reorg 风险识别能力，业务层不直接依赖底层回执细节，
 * 而是消费这个标准化后的核验结果。</p>
 */
@Data
public class DepositLogChainCheckResult {

    /**
     * 链上日志仍然存在。
     */
    public static final String PRESENT = "PRESENT";

    /**
     * 链上日志存在 reorg 风险。
     */
    public static final String REORG_SUSPECTED = "REORG_SUSPECTED";

    /**
     * 当前网关不支持充值日志核验。
     */
    public static final String UNSUPPORTED = "UNSUPPORTED";

    /**
     * 当前核验状态。
     */
    private String status;

    /**
     * 结果说明。
     */
    private String reason;

    /**
     * 创建“链上日志仍然存在”的结果。
     *
     * @return 核验结果
     */
    public static DepositLogChainCheckResult present() {
        DepositLogChainCheckResult result = new DepositLogChainCheckResult();
        result.setStatus(PRESENT);
        result.setReason("链上日志仍然存在");
        return result;
    }

    /**
     * 创建“存在 reorg 风险”的结果。
     *
     * @param reason 风险说明
     * @return 核验结果
     */
    public static DepositLogChainCheckResult reorgSuspected(String reason) {
        DepositLogChainCheckResult result = new DepositLogChainCheckResult();
        result.setStatus(REORG_SUSPECTED);
        result.setReason(reason);
        return result;
    }

    /**
     * 创建“不支持核验”的结果。
     *
     * @param reason 说明
     * @return 核验结果
     */
    public static DepositLogChainCheckResult unsupported(String reason) {
        DepositLogChainCheckResult result = new DepositLogChainCheckResult();
        result.setStatus(UNSUPPORTED);
        result.setReason(reason);
        return result;
    }

    /**
     * 当前结果是否表示链上日志仍然有效。
     *
     * @return true 表示日志仍然存在
     */
    public boolean isPresent() {
        return PRESENT.equals(status);
    }

    /**
     * 当前结果是否表示存在 reorg 风险。
     *
     * @return true 表示需要标记风险
     */
    public boolean isReorgSuspected() {
        return REORG_SUSPECTED.equals(status);
    }
}
