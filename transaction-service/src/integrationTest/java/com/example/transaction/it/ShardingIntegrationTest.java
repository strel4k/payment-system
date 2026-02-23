package com.example.transaction.it;

import com.example.transaction.dto.CreateWalletRequest;
import com.example.transaction.dto.TransactionConfirmRequest;
import com.example.transaction.dto.TransactionConfirmResponse;
import com.example.transaction.dto.TransactionInitRequest;
import com.example.transaction.dto.TransactionInitResponse;
import com.example.transaction.dto.WalletResponse;
import com.example.transaction.kafka.TransactionEventProducer;
import com.example.transaction.repository.WalletRepository;
import com.example.transaction.service.TransactionService;
import com.example.transaction.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sharding Integration Tests")
class ShardingIntegrationTest extends AbstractShardingIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository;

    @MockitoBean
    private TransactionEventProducer eventProducer;

    private static final UUID WALLET_TYPE_UID =
            UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    private static final UUID WALLET_TYPE_UID_2 =
            UUID.fromString("b1ffcd00-ad1c-5f09-cc7e-7cc0ce491b22");

    @BeforeEach
    void cleanShards() throws Exception {
        for (var shard : new org.testcontainers.containers.PostgreSQLContainer<?>[]{SHARD_0, SHARD_1}) {
            try (Connection conn = shard.createConnection("");
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM transactions");
                stmt.execute("DELETE FROM wallets");
            }
        }
    }

    // 1. Routing: разные пользователи попадают на разные шарды

    @Nested
    @DisplayName("Routing по user_uid")
    class RoutingTests {

        @Test
        @DisplayName("Кошельки двух пользователей с разными шардами попадают на разные БД")
        void wallets_differentUsers_routedToDifferentShards() throws Exception {
            UUID user0 = findUserForShard(0);
            UUID user1 = findUserForShard(1);

            createWallet(user0, "Wallet-U0");
            createWallet(user1, "Wallet-U1");

            assertThat(countRows(SHARD_0, "wallets")).isEqualTo(1);
            assertThat(countRows(SHARD_1, "wallets")).isEqualTo(1);
        }

        @Test
        @DisplayName("Все кошельки одного пользователя всегда попадают на один шард")
        void wallets_sameUser_alwaysSameShard() throws Exception {
            UUID userUid = UUID.randomUUID();
            int expectedShard = expectedShard(userUid);

            // Используем два разных типа кошелька, чтобы не получить DuplicateWalletException
            createWallet(userUid, "Wallet-A", WALLET_TYPE_UID);
            createWallet(userUid, "Wallet-B", WALLET_TYPE_UID_2);

            var primary = expectedShard == 0 ? SHARD_0 : SHARD_1;
            var other = expectedShard == 0 ? SHARD_1 : SHARD_0;

            assertThat(countRows(primary, "wallets"))
                    .as("Оба кошелька должны быть на шарде %d", expectedShard).isEqualTo(2);
            assertThat(countRows(other, "wallets"))
                    .as("На другом шарде кошельков быть не должно").isEqualTo(0);
        }
    }

    // 2. Транзакции следуют за пользователем

    @Nested
    @DisplayName("Транзакции хранятся на том же шарде, что и кошелёк")
    class TransactionShardingTests {

        @Test
        @DisplayName("Транзакция deposit записывается на шард пользователя")
        void deposit_storedOnSameShardAsWallet() throws Exception {
            UUID userUid = UUID.randomUUID();
            int expectedShard = expectedShard(userUid);

            WalletResponse wallet = createWallet(userUid, "TxWallet");

            TransactionInitResponse init = transactionService.initDeposit(
                    TransactionInitRequest.builder()
                            .walletUid(wallet.getUid())
                            .amount(new BigDecimal("100.00"))
                            .build()
            );

            transactionService.confirmDeposit(
                    TransactionConfirmRequest.builder()
                            .requestUid(init.getRequestUid())
                            .walletUid(wallet.getUid())
                            .amount(new BigDecimal("100.00"))
                            .build()
            );

            var primary = expectedShard == 0 ? SHARD_0 : SHARD_1;
            var other   = expectedShard == 0 ? SHARD_1 : SHARD_0;

            assertThat(countRows(primary, "transactions"))
                    .as("Транзакция должна быть на шарде %d", expectedShard).isEqualTo(1);
            assertThat(countRows(other, "transactions"))
                    .as("На другом шарде транзакций быть не должно").isEqualTo(0);
        }

        @Test
        @DisplayName("Несколько транзакций одного пользователя — все на одном шарде")
        void multipleTransactions_sameUser_sameShard() throws Exception {
            UUID userUid = UUID.randomUUID();
            int expectedShard = expectedShard(userUid);

            WalletResponse wallet = createWallet(userUid, "MultiTxWallet");

            // Первый депозит
            TransactionInitResponse init1 = transactionService.initDeposit(
                    TransactionInitRequest.builder()
                            .walletUid(wallet.getUid())
                            .amount(new BigDecimal("200.00"))
                            .build()
            );
            transactionService.confirmDeposit(
                    TransactionConfirmRequest.builder()
                            .requestUid(init1.getRequestUid())
                            .walletUid(wallet.getUid())
                            .amount(new BigDecimal("200.00"))
                            .build()
            );

            // Второй депозит
            TransactionInitResponse init2 = transactionService.initDeposit(
                    TransactionInitRequest.builder()
                            .walletUid(wallet.getUid())
                            .amount(new BigDecimal("50.00"))
                            .build()
            );
            transactionService.confirmDeposit(
                    TransactionConfirmRequest.builder()
                            .requestUid(init2.getRequestUid())
                            .walletUid(wallet.getUid())
                            .amount(new BigDecimal("50.00"))
                            .build()
            );

            var primary = expectedShard == 0 ? SHARD_0 : SHARD_1;
            var other   = expectedShard == 0 ? SHARD_1 : SHARD_0;

            assertThat(countRows(primary, "transactions")).isEqualTo(2);
            assertThat(countRows(other, "transactions")).isEqualTo(0);
        }
    }

    // 3. Broadcast-таблица wallet_types

    @Nested
    @DisplayName("Broadcast-таблица wallet_types")
    class BroadcastTableTests {

        @Test
        @DisplayName("wallet_types присутствует на обоих шардах с одинаковым числом строк")
        void walletTypes_broadcastOnBothShards() throws Exception {
            long count0 = countRows(SHARD_0, "wallet_types");
            long count1 = countRows(SHARD_1, "wallet_types");

            assertThat(count0).as("wallet_types должна быть на shard0").isGreaterThan(0);
            assertThat(count1).as("wallet_types должна быть на shard1").isGreaterThan(0);
            assertThat(count0).isEqualTo(count1);
        }
    }

    // 4. Cross-shard агрегация через ShardingSphere

    @Nested
    @DisplayName("Cross-shard запросы")
    class CrossShardTests {

        @Test
        @DisplayName("findAll возвращает кошельки с обоих шардов")
        void findAll_aggregatesFromBothShards() throws Exception {
            UUID user0 = findUserForShard(0);
            UUID user1 = findUserForShard(1);

            createWallet(user0, "WalletShard0");
            createWallet(user1, "WalletShard1");

            assertThat(walletRepository.findAll()).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("findByUserUid возвращает только кошельки запрошенного пользователя")
        void findByUserUid_returnsOnlyTargetUser() {
            UUID targetUser = UUID.randomUUID();
            UUID otherUser  = UUID.randomUUID();

            createWallet(targetUser, "Target-Wallet");
            createWallet(otherUser, "Other-Wallet");

            var result = walletRepository.findByUserUid(targetUser);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserUid()).isEqualTo(targetUser);
        }
    }

    // ── Helpers ─────────

    private WalletResponse createWallet(UUID userUid, String name) {
        return createWallet(userUid, name, WALLET_TYPE_UID);
    }

    private WalletResponse createWallet(UUID userUid, String name, UUID walletTypeUid) {
        return walletService.createWallet(
                CreateWalletRequest.builder()
                        .userUid(userUid)
                        .walletTypeUid(walletTypeUid)
                        .name(name)
                        .build()
        );
    }

    private long countRows(org.testcontainers.containers.PostgreSQLContainer<?> shard,
                           String table) throws Exception {
        try (Connection conn = shard.createConnection("");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getLong(1);
        }
    }

     // Вычисляет номер шарда для userUid по той же логике что ShardingSphere INLINE

    private int expectedShard(UUID userUid) {
        return Math.abs(userUid.hashCode()) % 2;
    }

     // Возвращает UUID, гарантированно попадающий на нужный шард.

    private UUID findUserForShard(int targetShard) {
        for (int i = 0; i < 1000; i++) {
            UUID candidate = UUID.randomUUID();
            if (expectedShard(candidate) == targetShard) return candidate;
        }
        throw new IllegalStateException("Не удалось найти UUID для шарда " + targetShard);
    }
}