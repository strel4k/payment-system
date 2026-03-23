package com.example.paymentservice.it;

import com.example.paymentservice.it.config.AbstractIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodIT extends AbstractIT {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String USER     = "test-user";
    private static final String PASSWORD = "test-password";

    @Test
    @DisplayName("GET /payment-methods/USD/USA — возвращает 200 и оба метода из seed")
    void getPaymentMethods_usdUsa_returnsBothMethods() {
        ResponseEntity<List> response = restTemplate
                .withBasicAuth(USER, PASSWORD)
                .getForEntity("/api/v1/payment-methods/USD/USA", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // CARD (is_all_currencies + is_all_countries) + BANK_TRANSFER (USD/USA)
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("GET /payment-methods/EUR/DEU — возвращает только CARD")
    void getPaymentMethods_eurDeu_returnsOnlyCard() {
        ResponseEntity<List> response = restTemplate
                .withBasicAuth(USER, PASSWORD)
                .getForEntity("/api/v1/payment-methods/EUR/DEU", List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);

        Map<?, ?> method = (Map<?, ?>) response.getBody().get(0);
        assertThat(method.get("provider_method_type")).isEqualTo("CARD");
    }

    @Test
    @DisplayName("GET /payment-methods/USD/USA — CARD содержит 4 required_fields")
    void getPaymentMethods_cardHasAllRequiredFields() {
        ResponseEntity<List> response = restTemplate
                .withBasicAuth(USER, PASSWORD)
                .getForEntity("/api/v1/payment-methods/USD/USA", List.class);

        assertThat(response.getBody()).isNotNull();

        Map<?, ?> card = ((List<Map<?, ?>>) response.getBody()).stream()
                .filter(m -> "CARD".equals(m.get("provider_method_type")))
                .findFirst()
                .orElseThrow();

        List<?> fields = (List<?>) card.get("required_fields");
        assertThat(fields).hasSize(4);

        List<String> fieldNames = fields.stream()
                .map(f -> (String) ((Map<?, ?>) f).get("name"))
                .toList();
        assertThat(fieldNames).containsExactlyInAnyOrder(
                "card_number", "card_holder", "expiry_date", "cvv"
        );
    }

    @Test
    @DisplayName("GET /payment-methods/USD/USA — без авторизации возвращает 401")
    void getPaymentMethods_noAuth_returns401() {
        ResponseEntity<Map> response = restTemplate
                .getForEntity("/api/v1/payment-methods/USD/USA", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /payment-methods/USD/USA — неверные credentials возвращают 401")
    void getPaymentMethods_wrongCredentials_returns401() {
        ResponseEntity<Map> response = restTemplate
                .withBasicAuth("wrong", "wrong")
                .getForEntity("/api/v1/payment-methods/USD/USA", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /actuator/health — доступен без авторизации")
    void actuatorHealth_noAuth_returns200() {
        ResponseEntity<Map> response = restTemplate
                .getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
    }
}