package com.example.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private WebhookDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        deliveryService = new WebhookDeliveryService(restTemplate, 0);
    }

    @Test
    @DisplayName("deliver — успешная доставка с первой попытки")
    void deliver_success_firstAttempt() {
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        deliveryService.deliver("http://merchant.example.com/notify", "TRANSACTION", 1L, "SUCCESS");

        verify(restTemplate, times(1)).postForEntity(any(String.class), any(), eq(String.class));
    }

    @Test
    @DisplayName("deliver — retry: 1 ошибка, 2-я попытка успешна")
    void deliver_retryOnce_secondAttemptSucceeds() {
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"))
                .thenReturn(ResponseEntity.ok("OK"));

        deliveryService.deliver("http://merchant.example.com/notify", "PAYOUT", 2L, "FAILED");

        verify(restTemplate, times(2)).postForEntity(any(String.class), any(), eq(String.class));
    }

    @Test
    @DisplayName("deliver — исчерпаны все 3 попытки, ошибка не пробрасывается")
    void deliver_allAttemptsExhausted_noExceptionThrown() {
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenThrow(new RestClientException("Timeout"));

        deliveryService.deliver("http://unreachable.example.com/notify", "TRANSACTION", 3L, "SUCCESS");

        verify(restTemplate, times(3)).postForEntity(any(String.class), any(), eq(String.class));
    }

    @Test
    @DisplayName("deliver — notification_url равен null, доставка пропускается")
    void deliver_nullUrl_skipped() {
        deliveryService.deliver(null, "TRANSACTION", 1L, "SUCCESS");

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("deliver — notification_url пустой, доставка пропускается")
    void deliver_blankUrl_skipped() {
        deliveryService.deliver("   ", "PAYOUT", 1L, "FAILED");

        verifyNoInteractions(restTemplate);
    }
}