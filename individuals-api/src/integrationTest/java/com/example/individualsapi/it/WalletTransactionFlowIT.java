package com.example.individualsapi.it;

import com.example.dto.TokenResponse;
import com.example.dto.person.PersonResponse;
import com.example.dto.transaction.CreateWalletRequest;
import com.example.dto.transaction.TransactionConfirmRequest;
import com.example.dto.transaction.TransactionConfirmResponse;
import com.example.dto.transaction.TransactionInitRequest;
import com.example.dto.transaction.TransactionInitResponse;
import com.example.dto.transaction.TransactionStatusResponse;
import com.example.dto.transaction.WalletResponse;
import com.example.individualsapi.client.PersonServiceClient;
import com.example.individualsapi.client.TransactionServiceClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class WalletTransactionFlowIT {

    // ── Keycloak ─────────

    @Container
    static final GenericContainer<?> KEYCLOAK = new GenericContainer<>("quay.io/keycloak/keycloak:26.2")
            .withExposedPorts(8080)
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
            .withEnv("KC_HEALTH_ENABLED", "true")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("realm-config.json"),
                    "/opt/keycloak/data/import/realm-config.json"
            )
            .withCommand("start-dev", "--import-realm")
            .waitingFor(
                    Wait.forHttp("/realms/individuals")
                            .forPort(8080)
                            .withStartupTimeout(Duration.ofMinutes(5))
            );

    @BeforeAll
    static void disableSsl() throws Exception {
        KEYCLOAK.execInContainer("sh", "-lc",
                "/opt/keycloak/bin/kcadm.sh config credentials " +
                        "--server http://localhost:8080 " +
                        "--realm master --user admin --password admin && " +
                        "/opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE && " +
                        "/opt/keycloak/bin/kcadm.sh update realms/individuals -s sslRequired=NONE"
        );
    }

    @DynamicPropertySource
    static void keycloakProps(DynamicPropertyRegistry r) {
        String baseUrl = "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
        r.add("keycloak.base-url", () -> baseUrl);
        r.add("keycloak.realm", () -> "individuals");
        r.add("keycloak.client-id", () -> "individuals-client");
        r.add("keycloak.client-secret", () -> "secret");
        r.add("keycloak.admin.username", () -> "admin");
        r.add("keycloak.admin.password", () -> "admin");
        r.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> baseUrl + "/realms/individuals");
    }

    // ── Зависимости ────────

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PersonServiceClient personServiceClient;

    @MockBean
    private TransactionServiceClient transactionServiceClient;

    // ── Константы ─────────

    private static final UUID WALLET_UID  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID WALLET_TYPE = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    private static final UUID REQUEST_UID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID TX_UID      = UUID.fromString("cccccccc-0000-0000-0000-000000000003");

    // ── Хелперы ──────────

    private String registerAndGetToken() {
        when(personServiceClient.createPerson(any()))
                .thenReturn(Mono.just(new PersonResponse()
                        .userId(UUID.randomUUID())
                        .email("stub@example.com")));

        String email = "it-" + System.currentTimeMillis() + "@example.com";

        TokenResponse tokens = webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email":"%s","password":"Qwe12345!","confirm_password":"Qwe12345!"}
                        """.formatted(email))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TokenResponse.class)
                .returnResult().getResponseBody();

        assertThat(tokens).isNotNull();
        assertThat(tokens.getAccessToken()).isNotBlank();
        return tokens.getAccessToken();
    }

    private WalletResponse walletStub(UUID userUid) {
        WalletResponse r = new WalletResponse();
        r.setUid(WALLET_UID);
        r.setUserUid(userUid);
        r.setWalletTypeUid(WALLET_TYPE);
        r.setName("USD Wallet");
        r.setStatus(WalletResponse.StatusEnum.ACTIVE);
        r.setBalance(BigDecimal.ZERO);
        return r;
    }

    private TransactionInitResponse initStub() {
        TransactionInitResponse r = new TransactionInitResponse();
        r.setRequestUid(REQUEST_UID);
        r.setAmount(BigDecimal.valueOf(100));
        r.setFee(BigDecimal.ZERO);
        r.setTotalAmount(BigDecimal.valueOf(100));
        return r;
    }

    private TransactionConfirmResponse confirmStub() {
        TransactionConfirmResponse r = new TransactionConfirmResponse();
        r.setTransactionUid(TX_UID);
        r.setStatus(TransactionConfirmResponse.StatusEnum.PENDING);
        r.setType(TransactionConfirmResponse.TypeEnum.DEPOSIT);
        r.setAmount(BigDecimal.valueOf(100));
        r.setFee(BigDecimal.ZERO);
        return r;
    }


    private TransactionStatusResponse statusStub() {
        TransactionStatusResponse r = new TransactionStatusResponse();
        r.setUid(TX_UID);
        r.setStatus(TransactionStatusResponse.StatusEnum.COMPLETED);
        r.setType(TransactionStatusResponse.TypeEnum.DEPOSIT);
        r.setAmount(BigDecimal.valueOf(100));
        return r;
    }

    // Wallets

    @Nested
    @DisplayName("POST /v1/wallets")
    class CreateWalletTests {

        @Test
        @DisplayName("Успешное создание → 201 с телом кошелька")
        void create_returns201() {
            String token = registerAndGetToken();
            UUID userUid = UUID.randomUUID();

            when(transactionServiceClient.createWallet(any(), anyString()))
                    .thenReturn(Mono.just(walletStub(userUid)));

            webTestClient.post()
                    .uri("/v1/wallets")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"walletTypeUid":"%s","name":"USD Wallet"}
                            """.formatted(WALLET_TYPE))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(WalletResponse.class)
                    .value(r -> {
                        assertThat(r.getUid()).isEqualTo(WALLET_UID);
                        assertThat(r.getStatus()).isEqualTo(WalletResponse.StatusEnum.ACTIVE);
                    });
        }

        @Test
        @DisplayName("Без токена → 401")
        void create_withoutToken_returns401() {
            webTestClient.post()
                    .uri("/v1/wallets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"walletTypeUid":"%s","name":"Test"}
                            """.formatted(WALLET_TYPE))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Конфликт от transaction-service → 5xx")
        void create_conflict_propagated() {
            String token = registerAndGetToken();

            when(transactionServiceClient.createWallet(any(), anyString()))
                    .thenReturn(Mono.error(
                            new org.springframework.web.reactive.function.client.WebClientResponseException(
                                    409, "Conflict", null, null, null)));

            webTestClient.post()
                    .uri("/v1/wallets")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"walletTypeUid":"%s","name":"Dup"}
                            """.formatted(WALLET_TYPE))
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    @Nested
    @DisplayName("GET /v1/wallets/{walletUid}")
    class GetWalletTests {

        @Test
        @DisplayName("Возвращает кошелёк → 200")
        void getById_returns200() {
            String token = registerAndGetToken();

            when(transactionServiceClient.getWallet(eq(WALLET_UID), anyString()))
                    .thenReturn(Mono.just(walletStub(UUID.randomUUID())));

            webTestClient.get()
                    .uri("/v1/wallets/{uid}", WALLET_UID)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(WalletResponse.class)
                    .value(r -> assertThat(r.getUid()).isEqualTo(WALLET_UID));
        }

        @Test
        @DisplayName("Несуществующий кошелёк → 5xx")
        void getById_notFound_propagated() {
            String token = registerAndGetToken();

            when(transactionServiceClient.getWallet(any(), anyString()))
                    .thenReturn(Mono.error(
                            new org.springframework.web.reactive.function.client.WebClientResponseException(
                                    404, "Not Found", null, null, null)));

            webTestClient.get()
                    .uri("/v1/wallets/{uid}", UUID.randomUUID())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("Без токена → 401")
        void getById_withoutToken_returns401() {
            webTestClient.get()
                    .uri("/v1/wallets/{uid}", WALLET_UID)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Nested
    @DisplayName("GET /v1/wallets")
    class GetUserWalletsTests {

        @Test
        @DisplayName("Возвращает список кошельков")
        void getAll_returnsList() {
            String token = registerAndGetToken();
            UUID userUid = UUID.randomUUID();

            WalletResponse w2 = walletStub(userUid);
            w2.setUid(UUID.randomUUID());

            when(transactionServiceClient.getUserWallets(anyString()))
                    .thenReturn(Flux.just(walletStub(userUid), w2));

            webTestClient.get()
                    .uri("/v1/wallets")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(WalletResponse.class)
                    .hasSize(2);
        }

        @Test
        @DisplayName("Пустой список → пустой массив")
        void getAll_empty() {
            String token = registerAndGetToken();

            when(transactionServiceClient.getUserWallets(anyString()))
                    .thenReturn(Flux.empty());

            webTestClient.get()
                    .uri("/v1/wallets")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(WalletResponse.class)
                    .hasSize(0);
        }

        @Test
        @DisplayName("Без токена → 401")
        void getAll_withoutToken_returns401() {
            webTestClient.get()
                    .uri("/v1/wallets")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    // Transactions

    @Nested
    @DisplayName("POST /v1/transactions/{type}/init")
    class InitTransactionTests {

        @Test
        @DisplayName("Deposit init → 200 с requestUid и нулевой комиссией")
        void initDeposit_returns200() {
            String token = registerAndGetToken();

            when(transactionServiceClient.initTransaction(eq("deposit"), any(), anyString()))
                    .thenReturn(Mono.just(initStub()));

            webTestClient.post()
                    .uri("/v1/transactions/deposit/init")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"walletUid":"%s","amount":100.00}
                            """.formatted(WALLET_UID))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TransactionInitResponse.class)
                    .value(r -> {
                        assertThat(r.getRequestUid()).isEqualTo(REQUEST_UID);
                        assertThat(r.getFee()).isEqualByComparingTo(BigDecimal.ZERO);
                    });
        }

        @Test
        @DisplayName("Withdrawal init → 200 с комиссией > 0")
        void initWithdrawal_returns200() {
            String token = registerAndGetToken();

            TransactionInitResponse withFee = initStub();
            withFee.setFee(BigDecimal.valueOf(1.0));
            withFee.setTotalAmount(BigDecimal.valueOf(101.0));

            when(transactionServiceClient.initTransaction(eq("withdrawal"), any(), anyString()))
                    .thenReturn(Mono.just(withFee));

            webTestClient.post()
                    .uri("/v1/transactions/withdrawal/init")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"walletUid":"%s","amount":100.00}
                            """.formatted(WALLET_UID))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TransactionInitResponse.class)
                    .value(r -> assertThat(r.getFee()).isGreaterThan(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Transfer init → 200")
        void initTransfer_returns200() {
            String token = registerAndGetToken();

            when(transactionServiceClient.initTransaction(eq("transfer"), any(), anyString()))
                    .thenReturn(Mono.just(initStub()));

            webTestClient.post()
                    .uri("/v1/transactions/transfer/init")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"walletUid":"%s","targetWalletUid":"%s","amount":50.00}
                            """.formatted(WALLET_UID, UUID.randomUUID()))
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Без токена → 401")
        void init_withoutToken_returns401() {
            webTestClient.post()
                    .uri("/v1/transactions/deposit/init")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"walletUid":"%s","amount":100.00}
                            """.formatted(WALLET_UID))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Nested
    @DisplayName("POST /v1/transactions/{type}/confirm")
    class ConfirmTransactionTests {

        @Test
        @DisplayName("Confirm deposit → 201 с transactionUid и статусом PENDING")
        void confirmDeposit_returns201() {
            String token = registerAndGetToken();

            when(transactionServiceClient.confirmTransaction(eq("deposit"), any(), anyString()))
                    .thenReturn(Mono.just(confirmStub()));

            webTestClient.post()
                    .uri("/v1/transactions/deposit/confirm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"requestUid":"%s","walletUid":"%s","amount":100.00}
                            """.formatted(REQUEST_UID, WALLET_UID))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(TransactionConfirmResponse.class)
                    .value(r -> {
                        assertThat(r.getTransactionUid()).isEqualTo(TX_UID);
                        assertThat(r.getStatus()).isEqualTo(TransactionConfirmResponse.StatusEnum.PENDING);
                        assertThat(r.getType()).isEqualTo(TransactionConfirmResponse.TypeEnum.DEPOSIT);
                    });
        }

        @Test
        @DisplayName("Просроченный request → 5xx")
        void confirm_expired_propagated() {
            String token = registerAndGetToken();

            when(transactionServiceClient.confirmTransaction(eq("deposit"), any(), anyString()))
                    .thenReturn(Mono.error(
                            new org.springframework.web.reactive.function.client.WebClientResponseException(
                                    400, "Bad Request", null, null, null)));

            webTestClient.post()
                    .uri("/v1/transactions/deposit/confirm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"requestUid":"%s","walletUid":"%s","amount":100.00}
                            """.formatted(UUID.randomUUID(), WALLET_UID))
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("Без токена → 401")
        void confirm_withoutToken_returns401() {
            webTestClient.post()
                    .uri("/v1/transactions/deposit/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                            {"requestUid":"%s","walletUid":"%s","amount":100.00}
                            """.formatted(REQUEST_UID, WALLET_UID))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Nested
    @DisplayName("GET /v1/transactions/{transactionUid}/status")
    class TransactionStatusTests {

        @Test
        @DisplayName("Возвращает статус COMPLETED → 200")
        void getStatus_returns200() {
            String token = registerAndGetToken();

            when(transactionServiceClient.getTransactionStatus(eq(TX_UID), anyString()))
                    .thenReturn(Mono.just(statusStub()));

            webTestClient.get()
                    .uri("/v1/transactions/{uid}/status", TX_UID)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TransactionStatusResponse.class)
                    .value(r -> {
                        assertThat(r.getUid()).isEqualTo(TX_UID);
                        assertThat(r.getStatus()).isEqualTo(TransactionStatusResponse.StatusEnum.COMPLETED);
                        assertThat(r.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
                    });
        }

        @Test
        @DisplayName("Несуществующая транзакция → 5xx")
        void getStatus_notFound_propagated() {
            String token = registerAndGetToken();

            when(transactionServiceClient.getTransactionStatus(any(), anyString()))
                    .thenReturn(Mono.error(
                            new org.springframework.web.reactive.function.client.WebClientResponseException(
                                    404, "Not Found", null, null, null)));

            webTestClient.get()
                    .uri("/v1/transactions/{uid}/status", UUID.randomUUID())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("Без токена → 401")
        void getStatus_withoutToken_returns401() {
            webTestClient.get()
                    .uri("/v1/transactions/{uid}/status", TX_UID)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    // Full E2E: register → create wallet → init deposit → confirm → status

    @Test
    @DisplayName("E2E: полный сценарий deposit flow проходит успешно")
    void fullDepositFlow_success() {
        // 1. Register
        String token = registerAndGetToken();
        UUID userUid = UUID.randomUUID();

        // 2. Create wallet
        when(transactionServiceClient.createWallet(any(), anyString()))
                .thenReturn(Mono.just(walletStub(userUid)));

        WalletResponse wallet = webTestClient.post()
                .uri("/v1/wallets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"walletTypeUid":"%s","name":"E2E Wallet"}
                        """.formatted(WALLET_TYPE))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WalletResponse.class)
                .returnResult().getResponseBody();

        assertThat(wallet).isNotNull();

        // 3. Init deposit
        when(transactionServiceClient.initTransaction(eq("deposit"), any(), anyString()))
                .thenReturn(Mono.just(initStub()));

        TransactionInitResponse init = webTestClient.post()
                .uri("/v1/transactions/deposit/init")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"walletUid":"%s","amount":250.00}
                        """.formatted(wallet.getUid()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TransactionInitResponse.class)
                .returnResult().getResponseBody();

        assertThat(init).isNotNull();
        assertThat(init.getRequestUid()).isNotNull();

        // 4. Confirm deposit
        when(transactionServiceClient.confirmTransaction(eq("deposit"), any(), anyString()))
                .thenReturn(Mono.just(confirmStub()));

        TransactionConfirmResponse confirmed = webTestClient.post()
                .uri("/v1/transactions/deposit/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"requestUid":"%s","walletUid":"%s","amount":250.00}
                        """.formatted(init.getRequestUid(), wallet.getUid()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TransactionConfirmResponse.class)
                .returnResult().getResponseBody();

        assertThat(confirmed).isNotNull();
        assertThat(confirmed.getTransactionUid()).isNotNull();

        // 5. Get status
        when(transactionServiceClient.getTransactionStatus(eq(confirmed.getTransactionUid()), anyString()))
                .thenReturn(Mono.just(statusStub()));

        webTestClient.get()
                .uri("/v1/transactions/{uid}/status", confirmed.getTransactionUid())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TransactionStatusResponse.class)
                .value(r -> assertThat(r.getStatus())
                        .isEqualTo(TransactionStatusResponse.StatusEnum.COMPLETED));
    }
}