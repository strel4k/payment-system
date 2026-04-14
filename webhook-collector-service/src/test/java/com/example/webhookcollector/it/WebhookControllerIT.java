package com.example.webhookcollector.it;

import com.example.webhookcollector.it.config.AbstractIT;
import com.example.webhookcollector.repository.PaymentProviderCallbackRepository;
import com.example.webhookcollector.repository.UnknownCallbackRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookControllerIT extends AbstractIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentProviderCallbackRepository paymentProviderCallbackRepository;

    @Autowired
    private UnknownCallbackRepository unknownCallbackRepository;

    @Value("${kafka.topics.payment-status-updated}")
    private String paymentStatusUpdatedTopic;

    private static final String ENDPOINT = "/api/v1/webhooks/payment-provider";

    @BeforeEach
    void cleanUp() {
        paymentProviderCallbackRepository.deleteAll();
        unknownCallbackRepository.deleteAll();
    }

    // ── Happy path ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /webhooks/payment-provider — известный тип → 200, сохранён в payment_provider_callbacks, событие опубликовано в Kafka")
    void receiveWebhook_knownType_returns200AndPublishesToKafka() {
        String providerTxUid = UUID.randomUUID().toString();
        String body = String.format(
                "{\"providerTransactionUid\":\"%s\",\"type\":\"PAYMENT_STATUS_UPDATED\",\"provider\":\"FPP\",\"status\":\"COMPLETED\"}",
                providerTxUid
        );

        Consumer<String, String> consumer = createConsumerAtLatest(paymentStatusUpdatedTopic);

        ResponseEntity<Void> response = post(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(paymentProviderCallbackRepository.findAll()).hasSize(1);
        assertThat(unknownCallbackRepository.findAll()).isEmpty();

        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        consumer.close();
        assertThat(records.count()).isEqualTo(1);
        assertThat(records.iterator().next().key()).isEqualTo(providerTxUid);
    }

    @Test
    @DisplayName("POST /webhooks/payment-provider — неизвестный тип → 200, сохранён в unknown_callbacks, в Kafka ничего нет")
    void receiveWebhook_unknownType_returns200AndNoKafkaEvent() {
        String body = "{\"providerTransactionUid\":null,\"type\":\"SOME_UNKNOWN_TYPE\",\"provider\":\"FPP\",\"status\":null}";

        Consumer<String, String> consumer = createConsumerAtLatest(paymentStatusUpdatedTopic);

        ResponseEntity<Void> response = post(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unknownCallbackRepository.findAll()).hasSize(1);
        assertThat(paymentProviderCallbackRepository.findAll()).isEmpty();

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));
        consumer.close();
        assertThat(records.count()).isZero();
    }

    // ── Безопасность ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /webhooks/payment-provider — неверный токен → 401, ничего не сохраняется")
    void receiveWebhook_invalidToken_returns401() {
        String body = "{\"type\":\"PAYMENT_STATUS_UPDATED\",\"provider\":\"FPP\",\"status\":\"COMPLETED\"}";

        ResponseEntity<Map> response = restTemplate.exchange(
                ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(body, buildHeaders("wrong-token", computeHmac(body, TEST_HMAC_SECRET))),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(paymentProviderCallbackRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST /webhooks/payment-provider — неверная подпись → 401, ничего не сохраняется")
    void receiveWebhook_invalidSignature_returns401() {
        String body = "{\"type\":\"PAYMENT_STATUS_UPDATED\",\"provider\":\"FPP\",\"status\":\"COMPLETED\"}";

        ResponseEntity<Map> response = restTemplate.exchange(
                ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(body, buildHeaders(TEST_WEBHOOK_TOKEN, "invalid-signature")),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(paymentProviderCallbackRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("POST /webhooks/payment-provider — подпись от другого тела → 401")
    void receiveWebhook_signatureForDifferentBody_returns401() {
        String body = "{\"type\":\"PAYMENT_STATUS_UPDATED\",\"provider\":\"FPP\",\"status\":\"COMPLETED\"}";

        ResponseEntity<Map> response = restTemplate.exchange(
                ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(body, buildHeaders(TEST_WEBHOOK_TOKEN, computeHmac("{\"type\":\"OTHER\"}", TEST_HMAC_SECRET))),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Actuator ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /actuator/health — доступен без авторизации")
    void actuatorHealth_returns200() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
    }

    // ── helpers ───────────────────────────────────────────────────

    private ResponseEntity<Void> post(String body) {
        return restTemplate.exchange(
                ENDPOINT, HttpMethod.POST,
                new HttpEntity<>(body, buildHeaders(TEST_WEBHOOK_TOKEN, computeHmac(body, TEST_HMAC_SECRET))),
                Void.class
        );
    }

    private HttpHeaders buildHeaders(String token, String signature) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Token", token);
        headers.set("X-Webhook-Signature", signature);
        return headers;
    }

    private Consumer<String, String> createConsumerAtLatest(String topic) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                KAFKA.getBootstrapServers(),
                "test-consumer-" + UUID.randomUUID(),
                "false"
        );
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(props)
                .createConsumer();

        List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                .map(p -> new TopicPartition(p.topic(), p.partition()))
                .toList();
        consumer.assign(partitions);
        consumer.seekToEnd(partitions);
        consumer.poll(Duration.ofMillis(100));
        return consumer;
    }

    private String computeHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}