// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

/**
 * @title Day8SimpleToken
 * @dev Day8 本地联调使用的最小 ERC20 示例合约。
 * 这里只保留 Java 监听最关心的余额表、Transfer 事件和 transfer 方法，避免被完整标准实现分散注意力。
 */
contract Day8SimpleToken {
    /**
     * @dev Token 名称。
     * 这个字段主要用于本地联调输出展示，不参与监听逻辑判断。
     */
    string public constant name = "Day8 Demo Token";

    /**
     * @dev Token 符号。
     * Java 监听脚本会把这个符号一起带进本地演示输出，方便和数据库记录对照。
     */
    string public constant symbol = "D8T";

    /**
     * @dev Token 精度。
     * Day8 监听层默认按最小单位落库，所以这里仍然显式保留 18 位精度。
     */
    uint8 public constant decimals = 18;

    /**
     * @dev 总供应量。
     * 构造时一次性铸造给初始持有人，便于后续直接发转账事件给 Java 监听器消费。
     */
    uint256 public totalSupply;

    /**
     * @dev 地址余额表。
     * 这是最小 ERC20 练习里真正会被 transfer 读写的持久化状态。
     */
    mapping(address => uint256) public balanceOf;

    /**
     * @dev ERC20 标准 Transfer 事件。
     * Java Day8 监听器就是按这个事件签名和 topic 结构做过滤与解码。
     */
    event Transfer(address indexed from, address indexed to, uint256 value);

    /**
     * @dev 部署时把初始供应量全部给到指定持有人。
     * 这里顺手抛出一条从零地址到持有人的 Transfer，保持和常见 ERC20 行为一致。
     *
     * @param initialHolder 初始持有人地址
     * @param initialSupply 初始供应量，单位为最小精度单位
     */
    constructor(address initialHolder, uint256 initialSupply) {
        require(initialHolder != address(0), "initial holder is zero");
        require(initialSupply > 0, "initial supply is zero");

        totalSupply = initialSupply;
        balanceOf[initialHolder] = initialSupply;

        emit Transfer(address(0), initialHolder, initialSupply);
    }

    /**
     * @dev 最小转账实现。
     * Day8 的目标是稳定打出 Transfer 日志，所以这里只保留零地址校验和余额校验两个核心分支。
     *
     * @param to 接收地址
     * @param amount 转账金额，单位为最小精度单位
     * @return 是否转账成功
     */
    function transfer(address to, uint256 amount) external returns (bool) {
        require(to != address(0), "receiver is zero");
        require(balanceOf[msg.sender] >= amount, "insufficient balance");

        unchecked {
            balanceOf[msg.sender] -= amount;
            balanceOf[to] += amount;
        }

        emit Transfer(msg.sender, to, amount);
        return true;
    }
}
