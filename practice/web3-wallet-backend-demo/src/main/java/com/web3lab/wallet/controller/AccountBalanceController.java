package com.web3lab.wallet.controller;

import com.web3lab.wallet.application.account.AccountBalanceAppService;
import com.web3lab.wallet.common.ApiResponse;
import com.web3lab.wallet.controller.dto.AccountBalanceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账户余额对外接口。
 */
@RestController
@RequestMapping("/account/balance")
public class AccountBalanceController {

    private final AccountBalanceAppService accountBalanceAppService;

    public AccountBalanceController(AccountBalanceAppService accountBalanceAppService) {
        this.accountBalanceAppService = accountBalanceAppService;
    }

    /**
     * 查询指定用户的余额快照。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码，未传时使用系统默认链
     * @param tokenSymbol 币种符号，未传时使用系统默认币种
     * @return 账户余额响应
     */
    @GetMapping("/{userId}")
    public ApiResponse<AccountBalanceResponse> getBalance(@PathVariable Long userId,
                                                          @RequestParam(required = false) String chain,
                                                          @RequestParam(required = false) String tokenSymbol) {
        return ApiResponse.success(accountBalanceAppService.getBalance(userId, chain, tokenSymbol));
    }
}
