CREATE TABLE IF NOT EXISTS wallet_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    chain VARCHAR(32) NOT NULL,
    token_symbol VARCHAR(32) NOT NULL,
    address VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_wallet_address_user_asset (user_id, chain, token_symbol),
    UNIQUE KEY uk_wallet_address (address)
);

CREATE TABLE IF NOT EXISTS account_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    chain VARCHAR(32) NOT NULL,
    token_symbol VARCHAR(32) NOT NULL,
    available_balance DECIMAL(38, 18) NOT NULL,
    frozen_balance DECIMAL(38, 18) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_account_balance_user_asset (user_id, chain, token_symbol)
);

CREATE TABLE IF NOT EXISTS account_bill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    chain VARCHAR(32) NOT NULL,
    token_symbol VARCHAR(32) NOT NULL,
    biz_type VARCHAR(32) NOT NULL,
    biz_id VARCHAR(64) NOT NULL,
    change_amount DECIMAL(38, 18) NOT NULL,
    available_after DECIMAL(38, 18) NOT NULL,
    frozen_after DECIMAL(38, 18) NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_account_bill_biz (biz_type, biz_id)
);

CREATE TABLE IF NOT EXISTS deposit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    chain VARCHAR(32) NOT NULL,
    token_symbol VARCHAR(32) NOT NULL,
    token_contract VARCHAR(64),
    from_address VARCHAR(64) NOT NULL,
    to_address VARCHAR(64) NOT NULL,
    tx_hash VARCHAR(80) NOT NULL,
    log_index INT NOT NULL,
    amount DECIMAL(38, 18) NOT NULL,
    block_number BIGINT NOT NULL,
    confirmations BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    credited_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_deposit_record_tx_log (tx_hash, log_index)
);

CREATE TABLE IF NOT EXISTS chain_scan_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chain VARCHAR(32) NOT NULL,
    task_name VARCHAR(64) NOT NULL,
    last_scanned_block BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_chain_scan_progress_task (chain, task_name)
);

CREATE TABLE IF NOT EXISTS withdraw_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    chain VARCHAR(32) NOT NULL,
    token_symbol VARCHAR(32) NOT NULL,
    to_address VARCHAR(64) NOT NULL,
    amount DECIMAL(38, 18) NOT NULL,
    fee DECIMAL(38, 18) NOT NULL,
    request_no VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    review_status VARCHAR(32),
    review_by VARCHAR(64),
    review_time DATETIME,
    tx_hash VARCHAR(80),
    nonce BIGINT,
    broadcast_retry_count INT NOT NULL DEFAULT 0,
    receipt_check_retry_count INT NOT NULL DEFAULT 0,
    fail_reason VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_withdraw_order_request_no (request_no)
);

CREATE TABLE IF NOT EXISTS account_reconcile_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(64) NOT NULL,
    task_batch_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    chain VARCHAR(32) NOT NULL,
    token_symbol VARCHAR(32) NOT NULL,
    available_balance DECIMAL(38, 18) NOT NULL,
    frozen_balance DECIMAL(38, 18) NOT NULL,
    total_balance DECIMAL(38, 18) NOT NULL,
    credited_deposit_amount DECIMAL(38, 18) NOT NULL,
    successful_withdraw_amount DECIMAL(38, 18) NOT NULL,
    pending_withdraw_frozen_amount DECIMAL(38, 18) NOT NULL,
    bill_asset_delta_amount DECIMAL(38, 18) NOT NULL,
    latest_bill_available_after DECIMAL(38, 18) NOT NULL,
    latest_bill_frozen_after DECIMAL(38, 18) NOT NULL,
    bill_count INT NOT NULL,
    consistent_with_business_tables TINYINT NOT NULL,
    consistent_with_asset_delta_bills TINYINT NOT NULL,
    consistent_with_latest_bill_snapshot TINYINT NOT NULL,
    consistent_with_pending_withdraws TINYINT NOT NULL,
    consistent TINYINT NOT NULL,
    mismatch_reason VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    KEY idx_account_reconcile_result_batch_no (task_batch_no),
    KEY idx_account_reconcile_result_user_batch (user_id, task_batch_no),
    KEY idx_account_reconcile_result_user_asset (user_id, chain, token_symbol)
);
