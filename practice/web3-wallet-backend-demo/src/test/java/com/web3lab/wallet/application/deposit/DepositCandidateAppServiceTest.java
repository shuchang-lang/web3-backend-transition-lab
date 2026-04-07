package com.web3lab.wallet.application.deposit;

import com.web3lab.wallet.domain.address.WalletAddress;
import com.web3lab.wallet.domain.deposit.DepositRecord;
import com.web3lab.wallet.infrastructure.persistence.DepositRecordMapper;
import com.web3lab.wallet.infrastructure.persistence.WalletAddressMapper;
import com.web3lab.wallet.infrastructure.web3.Erc20TransferLog;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositCandidateAppServiceTest {

    @Mock
    private WalletAddressMapper walletAddressMapper;

    @Mock
    private DepositRecordMapper depositRecordMapper;

    @InjectMocks
    private DepositCandidateAppService depositCandidateAppService;

    @Test
    void shouldStoreMatchedTransferAsPendingDeposit() {
        WalletAddress walletAddress = WalletAddress.create(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0x1234567890abcdef1234567890abcdef12345678",
                "ACTIVE"
        );
        when(walletAddressMapper.selectList(any())).thenReturn(List.of(walletAddress));

        Erc20TransferLog transferLog = Erc20TransferLog.create(
                "0xA0b86991C6218b36C1D19D4A2E9Eb0cE3606eB48",
                "0x1111111111111111111111111111111111111111",
                "0x1234567890ABCDEF1234567890ABCDEF12345678",
                new BigInteger("12500000"),
                "0xTxHash",
                0,
                123456L
        );

        int insertedCount = depositCandidateAppService.detectAndStore(
                "ETH_SEPOLIA",
                "USDT",
                6,
                List.of(transferLog)
        );

        assertEquals(1, insertedCount);
        ArgumentCaptor<DepositRecord> captor = ArgumentCaptor.forClass(DepositRecord.class);
        verify(depositRecordMapper).insert(captor.capture());
        DepositRecord depositRecord = captor.getValue();
        assertEquals(1001L, depositRecord.getUserId());
        assertEquals("PENDING_CONFIRM", depositRecord.getStatus());
        assertEquals(0, depositRecord.getAmount().compareTo(new BigDecimal("12.5")));
        assertEquals("0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", depositRecord.getTokenContract());
        assertEquals("0x1234567890abcdef1234567890abcdef12345678", depositRecord.getToAddress());
    }

    @Test
    void shouldIgnoreTransferWhenAddressDoesNotBelongToPlatform() {
        when(walletAddressMapper.selectList(any())).thenReturn(List.of());

        int insertedCount = depositCandidateAppService.detectAndStore(
                "ETH_SEPOLIA",
                "USDT",
                6,
                List.of(Erc20TransferLog.create(
                        "0xToken",
                        "0x1111111111111111111111111111111111111111",
                        "0x2222222222222222222222222222222222222222",
                        BigInteger.TEN,
                        "0xTxHash",
                        1,
                        123456L
                ))
        );

        assertEquals(0, insertedCount);
        verify(depositRecordMapper, never()).insert(org.mockito.ArgumentMatchers.<DepositRecord>any());
    }

    @Test
    void shouldIgnoreDuplicateTransferWhenUniqueKeyAlreadyExists() {
        WalletAddress walletAddress = WalletAddress.create(
                1001L,
                "ETH_SEPOLIA",
                "USDT",
                "0x1234567890abcdef1234567890abcdef12345678",
                "ACTIVE"
        );
        when(walletAddressMapper.selectList(any())).thenReturn(List.of(walletAddress));
        doThrow(new DuplicateKeyException("duplicate")).when(depositRecordMapper).insert(any(DepositRecord.class));

        int insertedCount = depositCandidateAppService.detectAndStore(
                "ETH_SEPOLIA",
                "USDT",
                6,
                List.of(Erc20TransferLog.create(
                        "0xToken",
                        "0x1111111111111111111111111111111111111111",
                        "0x1234567890abcdef1234567890abcdef12345678",
                        BigInteger.TEN,
                        "0xTxHash",
                        2,
                        123456L
                ))
        );

        assertEquals(0, insertedCount);
        verify(depositRecordMapper).insert(org.mockito.ArgumentMatchers.<DepositRecord>any());
    }
}
