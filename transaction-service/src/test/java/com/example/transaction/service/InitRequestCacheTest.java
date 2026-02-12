package com.example.transaction.service;

import com.example.transaction.config.AppProperties;
import com.example.transaction.entity.enums.PaymentType;
import com.example.transaction.exception.InvalidTransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class InitRequestCacheTest {

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private InitRequestCache cache;

    private InitRequest testRequest;
    private UUID requestUid;

    @BeforeEach
    void setUp() {
        lenient().when(appProperties.getInitRequestTtlMinutes()).thenReturn(15);

        requestUid = UUID.randomUUID();
        testRequest = InitRequest.builder()
                .requestUid(requestUid)
                .userUid(UUID.randomUUID())
                .walletUid(UUID.randomUUID())
                .type(PaymentType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .fee(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    @Nested
    @DisplayName("put() and get()")
    class PutAndGet {

        @Test
        @DisplayName("should store and retrieve request")
        void shouldStoreAndRetrieveRequest() {
            cache.put(testRequest);
            Optional<InitRequest> result = cache.get(requestUid);
            assertThat(result).isPresent();
            assertThat(result.get().getRequestUid()).isEqualTo(requestUid);
            assertThat(result.get().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should return empty for non-existent request")
        void shouldReturnEmptyForNonExistent() {
            Optional<InitRequest> result = cache.get(UUID.randomUUID());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for expired request")
        void shouldReturnEmptyForExpired() {
            InitRequest expiredRequest = InitRequest.builder()
                    .requestUid(requestUid)
                    .userUid(UUID.randomUUID())
                    .walletUid(UUID.randomUUID())
                    .type(PaymentType.DEPOSIT)
                    .amount(new BigDecimal("100.00"))
                    .fee(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("100.00"))
                    .createdAt(LocalDateTime.now().minusMinutes(20))
                    .expiresAt(LocalDateTime.now().minusMinutes(5))
                    .build();
            cache.put(expiredRequest);
            Optional<InitRequest> result = cache.get(requestUid);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAndRemove()")
    class GetAndRemove {

        @Test
        @DisplayName("should get and remove request")
        void shouldGetAndRemoveRequest() {
            cache.put(testRequest);
            InitRequest result = cache.getAndRemove(requestUid);
            assertThat(result.getRequestUid()).isEqualTo(requestUid);
            Optional<InitRequest> afterRemove = cache.get(requestUid);
            assertThat(afterRemove).isEmpty();
        }

        @Test
        @DisplayName("should throw for non-existent request")
        void shouldThrowForNonExistent() {
            assertThatThrownBy(() -> cache.getAndRemove(UUID.randomUUID()))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("should throw for expired request")
        void shouldThrowForExpired() {
            InitRequest expiredRequest = InitRequest.builder()
                    .requestUid(requestUid)
                    .userUid(UUID.randomUUID())
                    .walletUid(UUID.randomUUID())
                    .type(PaymentType.DEPOSIT)
                    .amount(new BigDecimal("100.00"))
                    .fee(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("100.00"))
                    .createdAt(LocalDateTime.now().minusMinutes(20))
                    .expiresAt(LocalDateTime.now().minusMinutes(5))
                    .build();
            cache.put(expiredRequest);
            assertThatThrownBy(() -> cache.getAndRemove(requestUid))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("expired");
        }
    }

    @Nested
    @DisplayName("exists()")
    class Exists {

        @Test
        @DisplayName("should return true for existing request")
        void shouldReturnTrueForExisting() {
            cache.put(testRequest);
            boolean exists = cache.exists(requestUid);
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false for non-existent request")
        void shouldReturnFalseForNonExistent() {
            boolean exists = cache.exists(UUID.randomUUID());
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("remove()")
    class Remove {

        @Test
        @DisplayName("should remove request")
        void shouldRemoveRequest() {
            cache.put(testRequest);
            cache.remove(requestUid);
            assertThat(cache.exists(requestUid)).isFalse();
        }
    }

    @Nested
    @DisplayName("size()")
    class Size {

        @Test
        @DisplayName("should return correct size")
        void shouldReturnCorrectSize() {
            assertThat(cache.size()).isZero();
            cache.put(testRequest);
            assertThat(cache.size()).isEqualTo(1);
            cache.put(InitRequest.builder()
                    .requestUid(UUID.randomUUID())
                    .userUid(UUID.randomUUID())
                    .walletUid(UUID.randomUUID())
                    .type(PaymentType.WITHDRAWAL)
                    .amount(new BigDecimal("50.00"))
                    .fee(new BigDecimal("0.50"))
                    .totalAmount(new BigDecimal("50.50"))
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build());
            assertThat(cache.size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("calculateExpiresAt()")
    class CalculateExpiresAt {

        @Test
        @DisplayName("should calculate expiration time based on TTL")
        void shouldCalculateExpirationTime() {
            LocalDateTime before = LocalDateTime.now().plusMinutes(14);
            LocalDateTime expiresAt = cache.calculateExpiresAt();
            LocalDateTime after = LocalDateTime.now().plusMinutes(16);
            assertThat(expiresAt).isAfter(before);
            assertThat(expiresAt).isBefore(after);
        }
    }

    @Nested
    @DisplayName("cleanupExpired()")
    class CleanupExpired {

        @Test
        @DisplayName("should remove expired requests")
        void shouldRemoveExpiredRequests() {
            cache.put(testRequest);
            InitRequest expiredRequest = InitRequest.builder()
                    .requestUid(UUID.randomUUID())
                    .userUid(UUID.randomUUID())
                    .walletUid(UUID.randomUUID())
                    .type(PaymentType.DEPOSIT)
                    .amount(new BigDecimal("50.00"))
                    .fee(BigDecimal.ZERO)
                    .totalAmount(new BigDecimal("50.00"))
                    .createdAt(LocalDateTime.now().minusMinutes(20))
                    .expiresAt(LocalDateTime.now().minusMinutes(5))
                    .build();
            cache.put(expiredRequest);
            assertThat(cache.size()).isEqualTo(2);
            cache.cleanupExpired();
            assertThat(cache.size()).isEqualTo(1);
            assertThat(cache.exists(requestUid)).isTrue();
        }
    }
}
