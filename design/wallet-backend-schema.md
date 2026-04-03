# Wallet Backend Schema

## 1. wallet_address

用途：维护充值地址与用户映射关系。

建议字段：

- `id`
- `user_id`
- `chain`
- `token_symbol`
- `address`
- `status`
- `created_at`

## 2. deposit_record

用途：记录候选充值与已确认充值。

建议字段：

- `id`
- `user_id`
- `address`
- `token_symbol`
- `tx_hash`
- `log_index`
- `amount`
- `block_number`
- `confirmations`
- `status`
- `created_at`
- `updated_at`

建议唯一键：

- `tx_hash + log_index`

## 3. withdraw_order

用途：记录提现申请和链上执行状态。

建议字段：

- `id`
- `user_id`
- `token_symbol`
- `to_address`
- `amount`
- `fee`
- `tx_hash`
- `nonce`
- `status`
- `fail_reason`
- `created_at`
- `updated_at`

## 4. account_balance

用途：保存用户资金主表。

建议字段：

- `id`
- `user_id`
- `token_symbol`
- `available_balance`
- `frozen_balance`
- `updated_at`

## 5. account_bill

用途：记录所有资金变动流水。

建议字段：

- `id`
- `user_id`
- `token_symbol`
- `biz_type`
- `biz_id`
- `change_amount`
- `balance_after`
- `remark`
- `created_at`

## 设计说明

- 充值成功：增加 `available_balance`
- 提现申请：`available_balance` 转 `frozen_balance`
- 提现成功：扣减 `frozen_balance`
- 提现失败：`frozen_balance` 退回 `available_balance`

