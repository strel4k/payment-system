package com.example.payment.it;

import com.example.payment.it.config.AbstractIT;
import com.example.payment.repository.MerchantRepository;
import com.example.payment.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionIT extends AbstractIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    private static final String MERCHANT_ID = "merchant-1";
    private static final String SECRET_KEY  = "secret123";

    @BeforeEach
    void cleanUp() {
        transactionRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/transactions — создаёт транзакцию, возвращает 201 и статус PENDING")
    void createTransaction_returns201WithPendingStatus() {
        Map<String, Object> body = Map.of(
                "amount", 150.00,
                "currency", "USD",
                "method", "CARD"
        );

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/transactions", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("PENDING");
        assertThat(response.getBody().get("id")).isNotNull();
        assertThat(response.getBody().get("merchantId")).isEqualTo(MERCHANT_ID);
    }

    @Test
    @DisplayName("POST /api/v1/transactions — без авторизации возвращает 401")
    void createTransaction_noAuth_returns401() {
        Map<String, Object> body = Map.of(
                "amount", 150.00,
                "currency", "USD",
                "method", "CARD"
        );

        ResponseEntity<Map> response = restTemplate
                .postForEntity("/api/v1/transactions", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /api/v1/transactions — невалидный метод оплаты возвращает 400")
    void createTransaction_invalidMethod_returns400() {
        Map<String, Object> body = Map.of(
                "amount", 150.00,
                "currency", "USD",
                "method", "UNKNOWN"
        );

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/transactions", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{id} — возвращает созданную транзакцию")
    void getTransaction_existingId_returns200() {
        Map<String, Object> body = Map.of(
                "amount", 200.00,
                "currency", "EUR",
                "method", "BANK_TRANSFER"
        );

        ResponseEntity<Map> created = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/transactions", body, Map.class);

        Integer id = (Integer) created.getBody().get("id");

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .getForEntity("/api/v1/transactions/" + id, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isEqualTo(id);
        assertThat(response.getBody().get("currency")).isEqualTo("EUR");
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{id} — несуществующий id возвращает 404")
    void getTransaction_nonExistingId_returns404() {
        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .getForEntity("/api/v1/transactions/999999", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GET /api/v1/transactions — возвращает список транзакций за период")
    void listTransactions_validDateRange_returns200WithList() {
        Map<String, Object> body = Map.of(
                "amount", 100.00,
                "currency", "USD",
                "method", "CARD"
        );
        restTemplate.withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/transactions", body, Map.class);

        String url = "/api/v1/transactions?start_date=2000-01-01T00:00:00&end_date=2999-12-31T23:59:59";

        ResponseEntity<Object[]> response = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .getForEntity(url, Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }
}