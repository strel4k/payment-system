package com.example.paymentservice.it;

import com.example.paymentservice.entity.PaymentOutboxStatus;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.it.config.AbstractIT;
import com.example.paymentservice.repository.PaymentOutboxRepository;
import com.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class PaymentOrchestrationIT extends AbstractIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentOutboxRepository outboxRepository;

    private static final String USER     = "test-user";
    private static final String PASSWORD = "test-password";

    private static final String FPP_SUCCESS_BODY = readFile("wiremock/__files/fpp-transaction-success.json");
    private static final String FPP_400_BODY     = readFile("wiremock/__files/fpp-transaction-400.json");

    @BeforeEach
    void cleanUp() {
        outboxRepository.deleteAll();
        paymentRepository.deleteAll();
        WIRE_MOCK.resetAll();
    }

    @Test
    @DisplayName("POST /payments — FPP 201 → сразу PENDING, outbox завершает в COMPLETED")
    void processPayment_fppSuccess_eventuallyCompleted() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/api/v1/transactions"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FPP_SUCCESS_BODY)));

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(USER, PASSWORD)
                .postForEntity("/api/v1/payments", buildRequest(1, 100.0, "USD"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("PENDING");

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var payments = paymentRepository.findAll();
                    assertThat(payments).hasSize(1);
                    assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.COMPLETED);
                    assertThat(payments.get(0).getExternalTransactionId()).isEqualTo("99");
                });

        var outbox = outboxRepository.findAll();
        assertThat(outbox).hasSize(1);
        assertThat(outbox.get(0).getStatus()).isEqualTo(PaymentOutboxStatus.COMPLETED);
        WIRE_MOCK.verify(1, postRequestedFor(urlEqualTo("/api/v1/transactions")));
    }

    @Test
    @DisplayName("POST /payments — FPP 400 → после retry платёж FAILED")
    void processPayment_fppReturns400_eventuallyFailed() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/api/v1/transactions"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(FPP_400_BODY)));

        String internalTxId = UUID.randomUUID().toString();
        Map<String, Object> req = buildRequest(1, 50.0, "USD");
        req.put("internalTransactionUid", internalTxId);

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(USER, PASSWORD)
                .postForEntity("/api/v1/payments", req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("PENDING");

        await().atMost(40, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var outbox = outboxRepository.findAll();
                    assertThat(outbox).hasSize(1);
                    assertThat(outbox.get(0).getStatus()).isEqualTo(PaymentOutboxStatus.FAILED);
                });

        var payment = paymentRepository.findByInternalTransactionUid(internalTxId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("POST /payments — несуществующий methodId → 404 синхронно, outbox не создаётся")
    void processPayment_unknownMethodId_returns404_noOutboxEntry() {
        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(USER, PASSWORD)
                .postForEntity("/api/v1/payments", buildRequest(9999, 100.0, "USD"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("METHOD_NOT_FOUND");
        assertThat(outboxRepository.findAll()).isEmpty();
        assertThat(paymentRepository.findAll()).isEmpty();
        WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/v1/transactions")));
    }

    @Test
    @DisplayName("POST /payments — без авторизации возвращает 401")
    void processPayment_noAuth_returns401() {
        ResponseEntity<Map> response = restTemplate
                .postForEntity("/api/v1/payments", buildRequest(1, 100.0, "USD"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/v1/transactions")));
    }

    @Test
    @DisplayName("POST /payments — неверные credentials возвращают 401")
    void processPayment_wrongCredentials_returns401() {
        ResponseEntity<Map> response = restTemplate
                .withBasicAuth("wrong", "wrong")
                .postForEntity("/api/v1/payments", buildRequest(1, 100.0, "USD"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/v1/transactions")));
    }

    // ==================== helpers ====================

    private Map<String, Object> buildRequest(int methodId, double amount, String currency) {
        return new java.util.HashMap<>(Map.of(
                "methodId", methodId,
                "internalTransactionUid", UUID.randomUUID().toString(),
                "amount", amount,
                "currency", currency
        ));
    }

    private static String readFile(String classpathPath) {
        try {
            return new ClassPathResource(classpathPath)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read test fixture: " + classpathPath, e);
        }
    }
}