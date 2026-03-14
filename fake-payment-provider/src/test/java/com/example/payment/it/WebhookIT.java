package com.example.payment.it;

import com.example.payment.entity.OperationStatus;
import com.example.payment.it.config.AbstractIT;
import com.example.payment.repository.TransactionRepository;
import com.example.payment.repository.WebhookAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookIT extends AbstractIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WebhookAuditRepository webhookAuditRepository;

    private static final String MERCHANT_ID = "merchant-1";
    private static final String SECRET_KEY  = "secret123";

    @BeforeEach
    void cleanUp() {
        webhookAuditRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /webhook/transaction — обновляет статус PENDING -> SUCCESS")
    void handleTransactionWebhook_updatesStatusToSuccess() {
        Integer transactionId = createTransaction();

        Map<String, Object> webhookBody = Map.of(
                "id", transactionId,
                "status", "SUCCESS"
        );

        ResponseEntity<Void> response = restTemplate
                .postForEntity("/webhook/transaction", webhookBody, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        com.example.payment.entity.Transaction updated =
                transactionRepository.findById(transactionId.longValue()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    @DisplayName("POST /webhook/transaction — обновляет статус PENDING -> FAILED")
    void handleTransactionWebhook_updatesStatusToFailed() {
        Integer transactionId = createTransaction();

        Map<String, Object> webhookBody = Map.of(
                "id", transactionId,
                "status", "FAILED"
        );

        ResponseEntity<Void> response = restTemplate
                .postForEntity("/webhook/transaction", webhookBody, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        com.example.payment.entity.Transaction updated =
                transactionRepository.findById(transactionId.longValue()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    @DisplayName("POST /webhook/transaction — идемпотентность: повторный вебхук только пишет audit")
    void handleTransactionWebhook_idempotency_onlyAuditOnRepeat() {
        Integer transactionId = createTransaction();

        Map<String, Object> webhookBody = Map.of(
                "id", transactionId,
                "status", "SUCCESS"
        );

        restTemplate.postForEntity("/webhook/transaction", webhookBody, Void.class);
        restTemplate.postForEntity("/webhook/transaction", webhookBody, Void.class);

        assertThat(webhookAuditRepository.findAll()).hasSize(2);

        com.example.payment.entity.Transaction updated =
                transactionRepository.findById(transactionId.longValue()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    @DisplayName("POST /webhook/transaction — не требует Basic Auth (открытый эндпоинт)")
    void handleTransactionWebhook_noAuthRequired() {
        Integer transactionId = createTransaction();

        Map<String, Object> webhookBody = Map.of(
                "id", transactionId,
                "status", "SUCCESS"
        );

        ResponseEntity<Void> response = restTemplate
                .postForEntity("/webhook/transaction", webhookBody, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("POST /webhook/transaction — несуществующий id возвращает 404")
    void handleTransactionWebhook_unknownId_returns404() {
        Map<String, Object> webhookBody = Map.of(
                "id", 999999,
                "status", "SUCCESS"
        );

        ResponseEntity<Map> response = restTemplate
                .postForEntity("/webhook/transaction", webhookBody, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Integer createTransaction() {
        Map<String, Object> body = Map.of(
                "amount", 100.00,
                "currency", "USD",
                "method", "CARD"
        );
        ResponseEntity<Map> created = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/transactions", body, Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (Integer) created.getBody().get("id");
    }
}