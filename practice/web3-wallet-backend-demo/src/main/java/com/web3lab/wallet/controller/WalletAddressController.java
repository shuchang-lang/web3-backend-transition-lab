package com.web3lab.wallet.controller;

import com.web3lab.wallet.application.address.WalletAddressAppService;
import com.web3lab.wallet.common.ApiResponse;
import com.web3lab.wallet.controller.dto.CreateWalletAddressRequest;
import com.web3lab.wallet.controller.dto.WalletAddressResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 充值地址对外接口。
 */
@RestController
@RequestMapping("/wallet/address")
public class WalletAddressController {

    private final WalletAddressAppService walletAddressAppService;

    public WalletAddressController(WalletAddressAppService walletAddressAppService) {
        this.walletAddressAppService = walletAddressAppService;
    }

    /**
     * 为指定用户分配充值地址。
     *
     * @param request 创建充值地址请求
     * @return 充值地址响应
     */
    @PostMapping
    public ApiResponse<WalletAddressResponse> allocateAddress(@Valid @RequestBody CreateWalletAddressRequest request) {
        return ApiResponse.success(walletAddressAppService.allocateAddress(request));
    }

    /**
     * 查询指定用户的充值地址。
     *
     * @param userId 平台用户 ID
     * @param chain 链编码，未传时使用系统默认链
     * @param tokenSymbol 币种符号，未传时使用系统默认币种
     * @return 充值地址响应
     */
    @GetMapping("/{userId}")
    public ApiResponse<WalletAddressResponse> getAddress(@PathVariable Long userId,
                                                         @RequestParam(required = false) String chain,
                                                         @RequestParam(required = false) String tokenSymbol) {
        return ApiResponse.success(walletAddressAppService.getAddress(userId, chain, tokenSymbol));
    }
}
