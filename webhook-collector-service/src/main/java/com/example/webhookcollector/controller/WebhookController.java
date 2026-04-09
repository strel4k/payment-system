package com.example.webhookcollector.controller;

import com.example.webhookcollector.controller.dto.WebhookPayload;
import com.example.webhookcollector.security.WebhookSecurityService;
import com.example.webhookcollector.service.WebhookService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookSecurityService securityService;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @PostMapping("/payment-provider")
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader("X-Webhook-Token") String token,
            @RequestHeader("X-Webhook-Signature") String signature,
            @RequestBody String rawBody) throws JsonProcessingException {

        securityService.validateToken(token);
        securityService.verifyHmacSignature(rawBody, signature);

        WebhookPayload payload = objectMapper.readValue(rawBody, WebhookPayload.class);

        log.info("Webhook accepted: type={} provider={} providerTransactionUid={}",
                payload.type(), payload.provider(), payload.providerTransactionUid());

        webhookService.processWebhook(rawBody, payload);

        return ResponseEntity.ok().build();
    }
}