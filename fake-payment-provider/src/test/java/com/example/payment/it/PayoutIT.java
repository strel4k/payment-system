package com.example.payment.it;

import com.example.payment.it.config.AbstractIT;
import com.example.payment.repository.PayoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PayoutIT extends AbstractIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PayoutRepository payoutRepository;

    private static final String MERCHANT_ID = "merchant-1";
    private static final String SECRET_KEY  = "secret123";

    @BeforeEach
    void cleanUp() {
        payoutRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/payouts — создаёт выплату, возвращает 201 и статус PENDING")
    void createPayout_returns201WithPendingStatus() {
        Map<String, Object> body = Map.of(
                "amount", 500.00,
                "currency", "USD"
        );

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/payouts", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("PENDING");
        assertThat(response.getBody().get("merchantId")).isEqualTo(MERCHANT_ID);
        assertThat(response.getBody().get("currency")).isEqualTo("USD");
    }

    @Test
    @DisplayName("POST /api/v1/payouts — без авторизации возвращает 401")
    void createPayout_noAuth_returns401() {
        Map<String, Object> body = Map.of(
                "amount", 500.00,
                "currency", "USD"
        );

        ResponseEntity<Map> response = restTemplate
                .postForEntity("/api/v1/payouts", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /api/v1/payouts — нулевой amount возвращает 400")
    void createPayout_zeroAmount_returns400() {
        Map<String, Object> body = Map.of(
                "amount", 0.00,
                "currency", "USD"
        );

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/payouts", body, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("GET /api/v1/payouts/{id} — возвращает созданную выплату")
    void getPayout_existingId_returns200() {
        Map<String, Object> body = Map.of(
                "amount", 300.00,
                "currency", "EUR"
        );

        ResponseEntity<Map> created = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/payouts", body, Map.class);

        Integer id = (Integer) created.getBody().get("id");

        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .getForEntity("/api/v1/payouts/" + id, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("id")).isEqualTo(id);
        assertThat(response.getBody().get("currency")).isEqualTo("EUR");
    }

    @Test
    @DisplayName("GET /api/v1/payouts/{id} — несуществующий id возвращает 404")
    void getPayout_nonExistingId_returns404() {
        ResponseEntity<Map> response = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .getForEntity("/api/v1/payouts/999999", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GET /api/v1/payouts — без параметров дат возвращает все выплаты мерчанта")
    void listPayouts_noDates_returnsAllPayouts() {
        Map<String, Object> body = Map.of("amount", 100.00, "currency", "USD");
        restTemplate.withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/payouts", body, Map.class);
        restTemplate.withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/payouts", body, Map.class);

        ResponseEntity<Object[]> response = restTemplate
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .getForEntity("/api/v1/payouts", Object[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }
}