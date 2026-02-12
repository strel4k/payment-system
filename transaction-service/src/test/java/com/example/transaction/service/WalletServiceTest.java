package com.example.transaction.service;

import com.example.transaction.dto.CreateWalletRequest;
import com.example.transaction.dto.WalletResponse;
import com.example.transaction.entity.Wallet;
import com.example.transaction.entity.WalletType;
import com.example.transaction.entity.enums.WalletStatus;
import com.example.transaction.exception.DuplicateWalletException;
import com.example.transaction.exception.WalletNotFoundException;
import com.example.transaction.exception.WalletTypeNotFoundException;
import com.example.transaction.mapper.WalletMapper;
import com.example.transaction.repository.WalletRepository;
import com.example.transaction.repository.WalletTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Tests")
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTypeRepository walletTypeRepository;

    @Spy
    private WalletMapper walletMapper = new WalletMapper();

    @InjectMocks
    private WalletService walletService;

    private UUID userUid;
    private UUID walletTypeUid;
    private UUID walletUid;
    private WalletType walletType;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userUid = UUID.randomUUID();
        walletTypeUid = UUID.randomUUID();
        walletUid = UUID.randomUUID();

        walletType = WalletType.builder()
                .name("USD Wallet")
                .currencyCode("USD")
                .status("ACTIVE")
                .build();
        walletType.setUid(walletTypeUid);

        wallet = Wallet.builder()
                .name("My USD Wallet")
                .walletType(walletType)
                .userUid(userUid)
                .status(WalletStatus.ACTIVE)
                .balance(BigDecimal.valueOf(100.00))
                .build();
        wallet.setUid(walletUid);
        wallet.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createWallet")
    class CreateWalletTests {

        @Test
        @DisplayName("should create wallet successfully")
        void shouldCreateWalletSuccessfully() {
            // Given
            CreateWalletRequest request = CreateWalletRequest.builder()
                    .userUid(userUid)
                    .walletTypeUid(walletTypeUid)
                    .name("My USD Wallet")
                    .build();

            when(walletTypeRepository.findById(walletTypeUid)).thenReturn(Optional.of(walletType));
            when(walletRepository.existsByUserUidAndWalletTypeUid(userUid, walletTypeUid)).thenReturn(false);
            when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

            // When
            WalletResponse response = walletService.createWallet(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUid()).isEqualTo(walletUid);
            assertThat(response.getUserUid()).isEqualTo(userUid);
            assertThat(response.getName()).isEqualTo("My USD Wallet");
            assertThat(response.getStatus()).isEqualTo(WalletResponse.StatusEnum.ACTIVE);
            assertThat(response.getCurrencyCode()).isEqualTo("USD");

            // Verify wallet was created with correct values
            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(walletCaptor.capture());

            Wallet savedWallet = walletCaptor.getValue();
            assertThat(savedWallet.getName()).isEqualTo("My USD Wallet");
            assertThat(savedWallet.getUserUid()).isEqualTo(userUid);
            assertThat(savedWallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
            assertThat(savedWallet.getBalance()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw WalletTypeNotFoundException when wallet type not found")
        void shouldThrowWalletTypeNotFoundException() {
            // Given
            CreateWalletRequest request = CreateWalletRequest.builder()
                    .userUid(userUid)
                    .walletTypeUid(walletTypeUid)
                    .name("My Wallet")
                    .build();

            when(walletTypeRepository.findById(walletTypeUid)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> walletService.createWallet(request))
                    .isInstanceOf(WalletTypeNotFoundException.class)
                    .hasMessageContaining(walletTypeUid.toString());

            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw DuplicateWalletException when user already has wallet of this type")
        void shouldThrowDuplicateWalletException() {
            // Given
            CreateWalletRequest request = CreateWalletRequest.builder()
                    .userUid(userUid)
                    .walletTypeUid(walletTypeUid)
                    .name("My Wallet")
                    .build();

            when(walletTypeRepository.findById(walletTypeUid)).thenReturn(Optional.of(walletType));
            when(walletRepository.existsByUserUidAndWalletTypeUid(userUid, walletTypeUid)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> walletService.createWallet(request))
                    .isInstanceOf(DuplicateWalletException.class)
                    .hasMessageContaining(userUid.toString())
                    .hasMessageContaining(walletTypeUid.toString());

            verify(walletRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getWallet")
    class GetWalletTests {

        @Test
        @DisplayName("should return wallet when found")
        void shouldReturnWalletWhenFound() {
            // Given
            when(walletRepository.findByIdWithWalletType(walletUid)).thenReturn(Optional.of(wallet));

            // When
            WalletResponse response = walletService.getWallet(walletUid);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUid()).isEqualTo(walletUid);
            assertThat(response.getUserUid()).isEqualTo(userUid);
            assertThat(response.getBalance()).isEqualTo(BigDecimal.valueOf(100.00));
        }

        @Test
        @DisplayName("should throw WalletNotFoundException when not found")
        void shouldThrowWalletNotFoundException() {
            // Given
            when(walletRepository.findByIdWithWalletType(walletUid)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> walletService.getWallet(walletUid))
                    .isInstanceOf(WalletNotFoundException.class)
                    .hasMessageContaining(walletUid.toString());
        }
    }

    @Nested
    @DisplayName("getWalletsByUser")
    class GetWalletsByUserTests {

        @Test
        @DisplayName("should return wallets for user")
        void shouldReturnWalletsForUser() {
            // Given
            Wallet wallet2 = Wallet.builder()
                    .name("My EUR Wallet")
                    .walletType(walletType)
                    .userUid(userUid)
                    .status(WalletStatus.ACTIVE)
                    .balance(BigDecimal.valueOf(200.00))
                    .build();
            wallet2.setUid(UUID.randomUUID());
            wallet2.setCreatedAt(LocalDateTime.now());

            when(walletRepository.findByUserUid(userUid)).thenReturn(List.of(wallet, wallet2));

            // When
            List<WalletResponse> responses = walletService.getWalletsByUser(userUid);

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(WalletResponse::getUserUid)
                    .containsOnly(userUid);
        }

        @Test
        @DisplayName("should return empty list when user has no wallets")
        void shouldReturnEmptyListWhenNoWallets() {
            // Given
            when(walletRepository.findByUserUid(userUid)).thenReturn(Collections.emptyList());

            // When
            List<WalletResponse> responses = walletService.getWalletsByUser(userUid);

            // Then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("getWalletForUpdate")
    class GetWalletForUpdateTests {

        @Test
        @DisplayName("should return wallet with lock")
        void shouldReturnWalletWithLock() {
            // Given
            when(walletRepository.findByIdForUpdate(walletUid)).thenReturn(Optional.of(wallet));

            // When
            Wallet result = walletService.getWalletForUpdate(walletUid);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUid()).isEqualTo(walletUid);
            verify(walletRepository).findByIdForUpdate(walletUid);
        }

        @Test
        @DisplayName("should throw WalletNotFoundException when not found")
        void shouldThrowWalletNotFoundExceptionForUpdate() {
            // Given
            when(walletRepository.findByIdForUpdate(walletUid)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> walletService.getWalletForUpdate(walletUid))
                    .isInstanceOf(WalletNotFoundException.class);
        }
    }
}