package com.example.transaction;

import com.example.transaction.dto.CreateWalletRequest;
import com.example.transaction.dto.TransactionConfirmRequest;
import com.example.transaction.dto.TransactionInitRequest;
import com.example.transaction.dto.TransactionInitResponse;
import com.example.transaction.dto.WalletResponse;
import com.example.transaction.entity.Transaction;
import com.example.transaction.entity.WalletType;
import com.example.transaction.kafka.TransactionEventProducer;
import com.example.transaction.repository.TransactionRepository;
import com.example.transaction.repository.WalletRepository;
import com.example.transaction.repository.WalletTypeRepository;
import com.example.transaction.service.TransactionService;
import com.example.transaction.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class TransactionRollbackTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @MockitoSpyBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private TransactionEventProducer eventProducer;

    private UUID userUid;
    private WalletResponse wallet;

    @BeforeEach
    void setUp() {

        Mockito.reset(transactionRepository);

        userUid = UUID.randomUUID();

        WalletType walletType = new WalletType();
        walletType.setName("USD Wallet");
        walletType.setCurrencyCode("USD");
        walletType.setStatus("ACTIVE");
        walletTypeRepository.save(walletType);

        wallet = walletService.createWallet(
                CreateWalletRequest.builder()
                        .userUid(userUid)
                        .walletTypeUid(walletType.getUid())
                        .name("Rollback Test Wallet")
                        .build()
        );

        com.example.transaction.entity.Wallet w = walletRepository.findById(wallet.getUid())
                .orElseThrow();
        w.credit(new BigDecimal("500.00"));
        walletRepository.save(w);
    }

    @Test
    @DisplayName("confirmDeposit — wallet balance must NOT change when transaction.save() throws")
    void shouldRollbackDepositWhenTransactionSaveFails() {
        BigDecimal balanceBefore = walletRepository.findById(wallet.getUid())
                .orElseThrow().getBalance();

        doThrow(new RuntimeException("Simulated DB failure during transaction save"))
                .when(transactionRepository).save(any(Transaction.class));

        TransactionInitResponse initResponse = transactionService.initDeposit(
                TransactionInitRequest.builder()
                        .walletUid(wallet.getUid())
                        .amount(new BigDecimal("200.00"))
                        .build()
        );

        assertThatThrownBy(() ->
                transactionService.confirmDeposit(
                        TransactionConfirmRequest.builder()
                                .requestUid(initResponse.getRequestUid())
                                .walletUid(wallet.getUid())
                                .amount(new BigDecimal("200.00"))
                                .build()
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated DB failure");

        BigDecimal balanceAfter = walletRepository.findById(wallet.getUid())
                .orElseThrow().getBalance();

        assertThat(balanceAfter)
                .as("Wallet balance must be rolled back to its pre-deposit value")
                .isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("confirmWithdrawal — wallet balance must NOT change when transaction.save() throws")
    void shouldRollbackWithdrawalWhenTransactionSaveFails() {
        BigDecimal balanceBefore = walletRepository.findById(wallet.getUid())
                .orElseThrow().getBalance();

        assertThat(balanceBefore)
                .as("Pre-condition: wallet must have funds before withdrawal test")
                .isGreaterThan(BigDecimal.ZERO);

        doThrow(new RuntimeException("Simulated DB failure during transaction save"))
                .when(transactionRepository).save(any(Transaction.class));

        TransactionInitResponse initResponse = transactionService.initWithdrawal(
                TransactionInitRequest.builder()
                        .walletUid(wallet.getUid())
                        .amount(new BigDecimal("100.00"))
                        .build()
        );

        assertThatThrownBy(() ->
                transactionService.confirmWithdrawal(
                        TransactionConfirmRequest.builder()
                                .requestUid(initResponse.getRequestUid())
                                .walletUid(wallet.getUid())
                                .amount(new BigDecimal("100.00"))
                                .build()
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated DB failure");

        BigDecimal balanceAfter = walletRepository.findById(wallet.getUid())
                .orElseThrow().getBalance();

        assertThat(balanceAfter)
                .as("Wallet balance must be rolled back — debit must not persist when tx.save() fails")
                .isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("No Transaction record must exist in DB after a rollback")
    void shouldNotPersistTransactionRecordAfterRollback() {
        long countBefore = transactionRepository.count();

        doThrow(new RuntimeException("Simulated DB failure during transaction save"))
                .when(transactionRepository).save(any(Transaction.class));

        TransactionInitResponse initResponse = transactionService.initDeposit(
                TransactionInitRequest.builder()
                        .walletUid(wallet.getUid())
                        .amount(new BigDecimal("50.00"))
                        .build()
        );

        assertThatThrownBy(() ->
                transactionService.confirmDeposit(
                        TransactionConfirmRequest.builder()
                                .requestUid(initResponse.getRequestUid())
                                .walletUid(wallet.getUid())
                                .amount(new BigDecimal("50.00"))
                                .build()
                )
        ).isInstanceOf(RuntimeException.class);

        long countAfter = transactionRepository.count();
        assertThat(countAfter)
                .as("No orphan Transaction rows must remain after rollback")
                .isEqualTo(countBefore);
    }
}