package com.example.payment.it;

import com.example.payment.it.config.AbstractIT;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class WebhookDeliveryIT extends AbstractIT {

    private static final String MERCHANT_ID   = "merchant-1";
    private static final String SECRET_KEY    = "secret123";
    private static final int    ASYNC_WAIT_MS = 5_000;

    @Autowired
    private TestRestTemplate http;

    private WireMockServer wireMock;
    private String notifyUrl;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        notifyUrl = "http://localhost:" + wireMock.port() + "/notify";
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("Webhook delivery — SUCCESS доставляется на notification_url")
    void delivery_success_callsNotificationUrl() throws InterruptedException {
        wireMock.stubFor(post(urlEqualTo("/notify"))
                .willReturn(aResponse().withStatus(200)));

        Integer txId = createTransactionWithNotificationUrl(notifyUrl);
        http.postForEntity("/webhook/transaction",
                Map.of("id", txId, "status", "SUCCESS"), Void.class);

        Thread.sleep(ASYNC_WAIT_MS);

        wireMock.verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/notify")));
    }

    @Test
    @DisplayName("Webhook delivery — FAILED доставляется на notification_url")
    void delivery_failed_callsNotificationUrl() throws InterruptedException {
        wireMock.stubFor(post(urlEqualTo("/notify"))
                .willReturn(aResponse().withStatus(200)));

        Integer txId = createTransactionWithNotificationUrl(notifyUrl);
        http.postForEntity("/webhook/transaction",
                Map.of("id", txId, "status", "FAILED"), Void.class);

        Thread.sleep(ASYNC_WAIT_MS);

        wireMock.verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/notify")));
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

        wireMock.verify(0, postRequestedFor(urlEqualTo("/notify")));
    }

    @Test
    @DisplayName("Webhook delivery — retry: после ошибки 500 повторяет запрос")
    void delivery_retry_onServerError() throws InterruptedException {
        wireMock.stubFor(post(urlEqualTo("/notify"))
                .inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("retried"));

        wireMock.stubFor(post(urlEqualTo("/notify"))
                .inScenario("retry")
                .whenScenarioStateIs("retried")
                .willReturn(aResponse().withStatus(200)));

        Integer txId = createTransactionWithNotificationUrl(notifyUrl);
        http.postForEntity("/webhook/transaction",
                Map.of("id", txId, "status", "SUCCESS"), Void.class);

        Thread.sleep(ASYNC_WAIT_MS);

        wireMock.verify(moreThanOrExactly(2), postRequestedFor(urlEqualTo("/notify")));
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