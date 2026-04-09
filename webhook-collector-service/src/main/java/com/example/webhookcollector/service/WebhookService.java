package com.example.webhookcollector.service;

import com.example.webhookcollector.controller.dto.WebhookPayload;
import com.example.webhookcollector.kafka.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookPersistenceService webhookPersistenceService;
    private final KafkaProducer kafkaProducer;

    public void processWebhook(String rawBody, WebhookPayload payload) {
        boolean shouldPublish = webhookPersistenceService.save(rawBody, payload);

        if (shouldPublish) {
            kafkaProducer.publishPaymentStatusUpdated(payload);
        }
    }
}