package com.example.transaction;

import com.example.transaction.dto.CreateWalletRequest;
import com.example.transaction.dto.TransactionConfirmRequest;
import com.example.transaction.dto.TransactionConfirmResponse;
import com.example.transaction.dto.TransactionInitRequest;
import com.example.transaction.dto.TransactionInitResponse;
import com.example.transaction.dto.TransactionStatusResponse;
import com.example.transaction.dto.WalletResponse;
import com.example.transaction.entity.WalletType;
import com.example.transaction.exception.InsufficientBalanceException;
import com.example.transaction.exception.InvalidTransactionException;
import com.example.transaction.kafka.TransactionEventProducer;
import com.example.transaction.repository.TransactionRepository;
import com.example.transaction.repository.WalletRepository;
import com.example.transaction.repository.WalletTypeRepository;
import com.example.transaction.service.TransactionService;
import com.example.transaction.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockitoBean
    private TransactionEventProducer eventProducer; // Mock Kafka producer

    private UUID userUid;
    private WalletType usdWalletType;
    private WalletResponse wallet;

    @BeforeEach
    void setUp() {
        userUid = UUID.randomUUID();

        // Create wallet type
        usdWalletType = new WalletType();
        usdWalletType.setName("USD Wallet");
        usdWalletType.setCurrencyCode("USD");
        usdWalletType.setStatus("ACTIVE");
        usdWalletType = walletTypeRepository.save(usdWalletType);

        // Create wallet
        CreateWalletRequest walletRequest = CreateWalletRequest.builder()
                .userUid(userUid)
                .walletTypeUid(usdWalletType.getUid())
                .name("Test Wallet")
                .build();
        wallet = walletService.createWallet(walletRequest);
    }

    @Nested
    @DisplayName("Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("should init and confirm deposit successfully")
        void shouldInitAndConfirmDeposit() {
            // Init deposit
            TransactionInitRequest initRequest = TransactionInitRequest.builder()
                    .walletUid(wallet.getUid())
                    .amount(new BigDecimal("100.00"))
                    .build();

            TransactionInitResponse initResponse = transactionService.initDeposit(initRequest);

            assertThat(initResponse).isNotNull();
            assertThat(initResponse.getRequestUid()).isNotNull();
            assertThat(initResponse.getFee()).isEqualByComparingTo(BigDecimal.ZERO); // 0% fee
            assertThat(initResponse.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(initResponse.getAvailable()).isTrue();

            // Confirm deposit
            TransactionConfirmRequest confirmRequest = TransactionConfirmRequest.builder()
                    .requestUid(initResponse.getRequestUid())
                    .walletUid(wallet.getUid())
                    .amount(new BigDecimal("100.00"))
                    .build();

            TransactionConfirmResponse confirmResponse = transactionService.confirmDeposit(confirmRequest);

            assertThat(confirmResponse).isNotNull();
            assertThat(confirmResponse.getTransactionUid()).isNotNull();
            assertThat(confirmResponse.getStatus()).isEqualTo(TransactionConfirmResponse.StatusEnum.PENDING);
            assertThat(confirmResponse.getType()).isEqualTo(TransactionConfirmResponse.TypeEnum.DEPOSIT);
        }
    }

    @Nested
    @DisplayName("Withdrawal Tests")
    class WithdrawalTests {

        @Test
        @DisplayName("should fail withdrawal init with insufficient balance")
        void shouldFailWithdrawalWithInsufficientBalance() {
            TransactionInitRequest initRequest = TransactionInitRequest.builder()
                    .walletUid(wallet.getUid())
                    .amount(new BigDecimal("100.00"))
                    .build();

            assertThatThrownBy(() -> transactionService.initWithdrawal(initRequest))
                    .isInstanceOf(InsufficientBalanceException.class);
        }
    }

    @Nested
    @DisplayName("Transfer Tests")
    class TransferTests {

        @Test
        @DisplayName("should fail transfer to same wallet")
        void shouldFailTransferToSameWallet() {
            TransactionInitRequest initRequest = TransactionInitRequest.builder()
                    .walletUid(wallet.getUid())
                    .targetWalletUid(wallet.getUid())
                    .amount(new BigDecimal("50.00"))
                    .build();

            assertThatThrownBy(() -> transactionService.initTransfer(initRequest))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("same wallet");
        }

        @Test
        @DisplayName("should fail transfer without target wallet")
        void shouldFailTransferWithoutTarget() {
            TransactionInitRequest initRequest = TransactionInitRequest.builder()
                    .walletUid(wallet.getUid())
                    .amount(new BigDecimal("50.00"))
                    .build();

            assertThatThrownBy(() -> transactionService.initTransfer(initRequest))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("Target wallet is required");
        }
    }

    @Nested
    @DisplayName("Transaction Status Tests")
    class StatusTests {

        @Test
        @DisplayName("should get transaction status after confirm")
        void shouldGetTransactionStatus() {
            // Init and confirm deposit
            TransactionInitRequest initRequest = TransactionInitRequest.builder()
                    .walletUid(wallet.getUid())
                    .amount(new BigDecimal("100.00"))
                    .build();

            TransactionInitResponse initResponse = transactionService.initDeposit(initRequest);

            TransactionConfirmRequest confirmRequest = TransactionConfirmRequest.builder()
                    .requestUid(initResponse.getRequestUid())
                    .walletUid(wallet.getUid())
                    .amount(new BigDecimal("100.00"))
                    .build();

            TransactionConfirmResponse confirmResponse = transactionService.confirmDeposit(confirmRequest);

            // Get status
            TransactionStatusResponse statusResponse = transactionService.getTransactionStatus(confirmResponse.getTransactionUid());

            assertThat(statusResponse).isNotNull();
            assertThat(statusResponse.getUid()).isEqualTo(confirmResponse.getTransactionUid());
            assertThat(statusResponse.getStatus()).isEqualTo(TransactionStatusResponse.StatusEnum.PENDING);
            assertThat(statusResponse.getType()).isEqualTo(TransactionStatusResponse.TypeEnum.DEPOSIT);
            assertThat(statusResponse.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(statusResponse.getCurrencyCode()).isEqualTo("USD");
        }
    }
}