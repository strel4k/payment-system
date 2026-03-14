package com.example.payment.it;

import com.example.payment.it.config.AbstractIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class WebhookDeliveryIT extends AbstractIT {

    private static final String MERCHANT_ID   = "merchant-1";
    private static final String SECRET_KEY    = "secret123";
    private static final String NOTIFY_URL    = "http://merchant.example.com/notify";
    private static final int    ASYNC_WAIT_MS = 3000;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestRestTemplate http;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    @DisplayName("Webhook delivery — SUCCESS доставляется на notification_url")
    void delivery_success_callsNotificationUrl() throws InterruptedException {
        mockServer.expect(once(), requestTo(NOTIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.type").value("TRANSACTION"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andRespond(withSuccess());

        Integer txId = createTransactionWithNotificationUrl(NOTIFY_URL);
        http.postForEntity("/webhook/transaction",
                Map.of("id", txId, "status", "SUCCESS"), Void.class);

        Thread.sleep(ASYNC_WAIT_MS);
        mockServer.verify();
    }

    @Test
    @DisplayName("Webhook delivery — FAILED доставляется на notification_url")
    void delivery_failed_callsNotificationUrl() throws InterruptedException {
        mockServer.expect(once(), requestTo(NOTIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andRespond(withSuccess());

        Integer txId = createTransactionWithNotificationUrl(NOTIFY_URL);
        http.postForEntity("/webhook/transaction",
                Map.of("id", txId, "status", "FAILED"), Void.class);

        Thread.sleep(ASYNC_WAIT_MS);
        mockServer.verify();
    }

    @Test
    @DisplayName("Webhook delivery — без notification_url исходящий запрос не выполняется")
    void delivery_noNotificationUrl_noOutboundRequest() throws InterruptedException {
        ResponseEntity<Map> created = http
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/transactions",
                        Map.of("amount", 50.0, "currency", "USD", "method", "CARD"), Map.class);
        Integer txId = (Integer) created.getBody().get("id");

        http.postForEntity("/webhook/transaction",
                Map.of("id", txId, "status", "SUCCESS"), Void.class);

        Thread.sleep(ASYNC_WAIT_MS);
        mockServer.verify();
    }

    @Test
    @DisplayName("Webhook delivery — retry: после ошибки повторяет запрос")
    void delivery_retry_onServerError() throws InterruptedException {
        mockServer.expect(once(), requestTo(NOTIFY_URL))
                .andRespond(withServerError());
        mockServer.expect(once(), requestTo(NOTIFY_URL))
                .andRespond(withSuccess());

        Integer txId = createTransactionWithNotificationUrl(NOTIFY_URL);
        http.postForEntity("/webhook/transaction",
                Map.of("id", txId, "status", "SUCCESS"), Void.class);

        Thread.sleep(ASYNC_WAIT_MS + 2_000); // +2s на backoff (1s) между попытками
        mockServer.verify();
    }

    private Integer createTransactionWithNotificationUrl(String notificationUrl) {
        ResponseEntity<Map> created = http
                .withBasicAuth(MERCHANT_ID, SECRET_KEY)
                .postForEntity("/api/v1/transactions",
                        Map.of("amount", 100.0, "currency", "USD",
                                "method", "CARD", "notificationUrl", notificationUrl),
                        Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (Integer) created.getBody().get("id");
    }
}