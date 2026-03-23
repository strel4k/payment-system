package com.example.paymentservice.it;

import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.it.config.AbstractIT;
import com.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentOrchestrationIT extends AbstractIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    private static final String USER     = "test-user";
    private static final String PASSWORD = "test-password";

    @BeforeEach
    void cleanUp() {
        paymentRepository.deleteAll();
        WIRE_MOCK.resetAll();
    }

    @Test
    @DisplayName("POST /payments — FPP возвращает 201 → ответ COMPLETED, в БД статус COMPLETED")
    void processPayment_fppSuccess_returnsCompleted() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/api/v1/transactions"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 99,
                                  "merchantId": "merchant-1",
                                  "amount": 100.0,
                                  "currency": "USD",
                                  "method": "CARD",
                                  "status": "PENDING",
                                  "createdAt": "2026-03-20T17:00:00Z"
                                }
                                """)));

        Map<String, Object> request = buildRequest(1, 100.0, "USD");

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(USER, PASSWORD)
                .postForEntity("/api/v1/payments", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("providerTransactionId")).isEqualTo("99");
        assertThat(response.getBody().get("status")).isEqualTo("COMPLETED");

        WIRE_MOCK.verify(1, postRequestedFor(urlEqualTo("/api/v1/transactions")));

        var payment = paymentRepository.findAll().stream()
                .filter(p -> "99".equals(p.getExternalTransactionId()))
                .findFirst();
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("POST /payments — FPP возвращает 400 → ответ 422, в БД статус FAILED")
    void processPayment_fppReturns400_returns422AndMarksFailed() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/api/v1/transactions"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "error": "VALIDATION_ERROR",
                                  "message": "Invalid method"
                                }
                                """)));

        String internalTxId = UUID.randomUUID().toString();
        Map<String, Object> request = buildRequest(1, 50.0, "USD");
        request.put("internalTransactionUid", internalTxId);

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(USER, PASSWORD)
                .postForEntity("/api/v1/payments", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().get("error")).isEqualTo("PAYMENT_FAILED");

        WIRE_MOCK.verify(1, postRequestedFor(urlEqualTo("/api/v1/transactions")));

        var payment = paymentRepository.findAll().stream()
                .filter(p -> internalTxId.equals(p.getInternalTransactionUid()))
                .findFirst();
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.get().getExternalTransactionId()).isNull();
    }

    @Test
    @DisplayName("POST /payments — несуществующий methodId → 404, FPP не вызывается")
    void processPayment_unknownMethodId_returns404_fppNotCalled() {
        Map<String, Object> request = buildRequest(9999, 100.0, "USD");

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(USER, PASSWORD)
                .postForEntity("/api/v1/payments", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("METHOD_NOT_FOUND");

        WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/v1/transactions")));
    }

    @Test
    @DisplayName("POST /payments — FPP недоступен (500) → 422, в БД статус FAILED")
    void processPayment_fppUnavailable_returns422AndMarksFailed() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/api/v1/transactions"))
                .willReturn(aResponse().withStatus(500)));

        String internalTxId = UUID.randomUUID().toString();
        Map<String, Object> request = buildRequest(1, 100.0, "USD");
        request.put("internalTransactionUid", internalTxId);

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(USER, PASSWORD)
                .postForEntity("/api/v1/payments", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        var payment = paymentRepository.findAll().stream()
                .filter(p -> internalTxId.equals(p.getInternalTransactionUid()))
                .findFirst();
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("POST /payments — без авторизации возвращает 401")
    void processPayment_noAuth_returns401() {
        Map<String, Object> request = buildRequest(1, 100.0, "USD");

        ResponseEntity<Map> response = restTemplate
                .postForEntity("/api/v1/payments", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        WIRE_MOCK.verify(0, postRequestedFor(urlEqualTo("/api/v1/transactions")));
    }

    @Test
    @DisplayName("POST /payments — неверные credentials возвращают 401")
    void processPayment_wrongCredentials_returns401() {
        Map<String, Object> request = buildRequest(1, 100.0, "USD");

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth("wrong-user", "wrong-password")
                .postForEntity("/api/v1/payments", request, Map.class);

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
}