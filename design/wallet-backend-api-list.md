# Wallet Backend API List

## 地址相关

### POST /wallet/address

用途：为用户创建或分配充值地址。

### GET /wallet/address/{userId}

用途：查询用户当前充值地址。

## 充值相关

### GET /deposit/list

用途：分页查询充值记录。

### GET /deposit/{txHash}

用途：按交易哈希查询充值记录。

## 提现相关

### POST /withdraw/apply

用途：提交提现申请。

### POST /withdraw/review/{id}

用途：审核提现单。

### GET /withdraw/{id}

用途：查询提现单详情。

## 余额相关

### GET /account/balance/{userId}

用途：查询用户余额与冻结余额。

## 后台任务接口（可选）

### POST /admin/deposit/recheck/{id}

用途：手动重查充值确认状态。

### POST /admin/withdraw/recheck/{id}

用途：手动重查提现回执状态。

