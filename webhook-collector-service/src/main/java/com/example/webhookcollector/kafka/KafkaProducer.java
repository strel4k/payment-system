package com.example.webhookcollector.kafka;

import com.example.webhookcollector.controller.dto.WebhookPayload;
import com.example.commonlibrary.event.PaymentStatusUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-status-updated}")
    private String paymentStatusUpdatedTopic;

    public void publishPaymentStatusUpdated(WebhookPayload payload) {
        PaymentStatusUpdatedEvent event = PaymentStatusUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .providerTransactionUid(payload.providerTransactionUid())
                .type(payload.type())
                .provider(payload.provider())
                .status(payload.status())
                .build();

        String key = payload.providerTransactionUid() != null
                ? payload.providerTransactionUid().toString()
                : UUID.randomUUID().toString();

        sendEvent(paymentStatusUpdatedTopic, key, event);
    }

    private void sendEvent(String topic, String key, Object payload) {
        log.debug("Sending event to topic={} key={} type={}",
                topic, key, payload.getClass().getSimpleName());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event to topic={} key={}", topic, key, ex);
            } else {
                log.debug("Event sent to topic={} key={} partition={} offset={}",
                        topic, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}