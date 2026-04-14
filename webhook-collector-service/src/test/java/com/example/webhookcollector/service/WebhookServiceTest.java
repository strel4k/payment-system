package com.example.webhookcollector.service;

import com.example.webhookcollector.controller.dto.WebhookPayload;
import com.example.webhookcollector.kafka.KafkaProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookPersistenceService webhookPersistenceService;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private WebhookService webhookService;

    private static final String   RAW_BODY        = "{\"type\":\"PAYMENT_STATUS_UPDATED\"}";
    private static final UUID     PROVIDER_TX_UID = UUID.randomUUID();

    @Test
    @DisplayName("processWebhook — persistenceService вернул true → Kafka вызван")
    void processWebhook_persistenceReturnsTrue_publishesToKafka() {
        WebhookPayload payload = new WebhookPayload(
                PROVIDER_TX_UID, "PAYMENT_STATUS_UPDATED", "FPP", "COMPLETED"
        );
        when(webhookPersistenceService.save(RAW_BODY, payload)).thenReturn(true);

        webhookService.processWebhook(RAW_BODY, payload);

        verify(webhookPersistenceService).save(RAW_BODY, payload);
        verify(kafkaProducer).publishPaymentStatusUpdated(payload);
    }

    @Test
    @DisplayName("processWebhook — persistenceService вернул false → Kafka не вызван")
    void processWebhook_persistenceReturnsFalse_doesNotPublishToKafka() {
        WebhookPayload payload = new WebhookPayload(
                PROVIDER_TX_UID, "UNKNOWN_TYPE", "FPP", null
        );
        when(webhookPersistenceService.save(RAW_BODY, payload)).thenReturn(false);

        webhookService.processWebhook(RAW_BODY, payload);

        verify(webhookPersistenceService).save(RAW_BODY, payload);
        verify(kafkaProducer, never()).publishPaymentStatusUpdated(payload);
    }
}