package com.example.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class WebhookDeliveryService {

    private final RestTemplate restTemplate;
    private final int maxAttempts;
    private final long baseDelayMs;

    @Autowired
    public WebhookDeliveryService(
            RestTemplate restTemplate,
            @Value("${webhook.delivery.max-attempts:3}") int maxAttempts,
            @Value("${webhook.delivery.base-delay-ms:1000}") long baseDelayMs) {
        this.restTemplate = restTemplate;
        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelayMs;
    }

    @Async
    public void deliver(String notificationUrl, String type, Long entityId, String status) {
        if (notificationUrl == null || notificationUrl.isBlank()) {
            log.debug("notification_url not set for {} id={}, skipping delivery", type, entityId);
            return;
        }

        HttpEntity<WebhookNotification> request = buildRequest(type, entityId, status);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restTemplate.postForEntity(notificationUrl, request, Void.class);
                log.info("Webhook delivered: type={} id={} status={} url={} attempt={}",
                        type, entityId, status, notificationUrl, attempt);
                return;
            } catch (Exception ex) {
                log.warn("Webhook delivery failed: type={} id={} url={} attempt={}/{} error={}",
                        type, entityId, notificationUrl, attempt, maxAttempts, ex.getMessage());

                if (attempt < maxAttempts) {
                    long delay = baseDelayMs * (1L << (attempt - 1));
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
                maxAttempts, type, entityId, notificationUrl);
    }

    private HttpEntity<WebhookNotification> buildRequest(String type, Long entityId, String status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(new WebhookNotification(type, entityId, status), headers);
    }
}