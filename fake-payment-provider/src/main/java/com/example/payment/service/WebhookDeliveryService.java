package com.example.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class WebhookDeliveryService {

    private static final int MAX_ATTEMPTS = 3;

    private final RestTemplate restTemplate;
    private final long baseDelayMs;

    @Autowired
    public WebhookDeliveryService(RestTemplate restTemplate) {
        this(restTemplate, 1_000);
    }

    WebhookDeliveryService(RestTemplate restTemplate, long baseDelayMs) {
        this.restTemplate = restTemplate;
        this.baseDelayMs = baseDelayMs;
    }

    @Async
    public void deliver(String notificationUrl, String type, Long entityId, String status) {
        if (notificationUrl == null || notificationUrl.isBlank()) {
            log.debug("notification_url not set for {} id={}, skipping delivery", type, entityId);
            return;
        }

        Map<String, Object> payload = Map.of(
                "type", type,
                "id", entityId,
                "status", status
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restTemplate.postForEntity(notificationUrl, request, String.class);
                log.info("Webhook delivered: type={} id={} status={} url={} attempt={}",
                        type, entityId, status, notificationUrl, attempt);
                return;
            } catch (Exception ex) {
                log.warn("Webhook delivery failed: type={} id={} url={} attempt={}/{} error={}",
                        type, entityId, notificationUrl, attempt, MAX_ATTEMPTS, ex.getMessage());

                if (attempt < MAX_ATTEMPTS) {
                    long delay = baseDelayMs * (1L << (attempt - 1)); // 1s, 2s, 4s
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Webhook delivery interrupted for {} id={}", type, entityId);
                        return;
                    }
                }
            }
        }

        log.error("Webhook delivery exhausted all {} attempts: type={} id={} url={}",
                MAX_ATTEMPTS, type, entityId, notificationUrl);
    }
}