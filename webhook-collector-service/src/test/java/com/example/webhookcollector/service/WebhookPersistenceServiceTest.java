package com.example.webhookcollector.service;

import com.example.webhookcollector.controller.dto.WebhookPayload;
import com.example.webhookcollector.entity.PaymentProviderCallback;
import com.example.webhookcollector.entity.UnknownCallback;
import com.example.webhookcollector.mapper.WebhookMapper;
import com.example.webhookcollector.repository.PaymentProviderCallbackRepository;
import com.example.webhookcollector.repository.UnknownCallbackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookPersistenceServiceTest {

    @Mock
    private PaymentProviderCallbackRepository paymentProviderCallbackRepository;

    @Mock
    private UnknownCallbackRepository unknownCallbackRepository;

    @Mock
    private WebhookMapper webhookMapper;

    @InjectMocks
    private WebhookPersistenceService webhookPersistenceService;

    private static final String RAW_BODY        = "{\"type\":\"PAYMENT_STATUS_UPDATED\"}";
    private static final UUID   PROVIDER_TX_UID = UUID.randomUUID();

    @Test
    @DisplayName("save — известный тип → сохраняет в payment_provider_callbacks, возвращает true")
    void save_knownType_savesCallbackAndReturnsTrue() {
        WebhookPayload payload = new WebhookPayload(
                PROVIDER_TX_UID, "PAYMENT_STATUS_UPDATED", "FPP", "COMPLETED"
        );
        PaymentProviderCallback mapped = new PaymentProviderCallback();
        when(webhookMapper.toPaymentProviderCallback(RAW_BODY, payload)).thenReturn(mapped);

        boolean result = webhookPersistenceService.save(RAW_BODY, payload);

        assertThat(result).isTrue();
        verify(paymentProviderCallbackRepository).save(mapped);
        verify(unknownCallbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("save — неизвестный тип → сохраняет в unknown_callbacks, возвращает false")
    void save_unknownType_savesUnknownCallbackAndReturnsFalse() {
        WebhookPayload payload = new WebhookPayload(
                PROVIDER_TX_UID, "SOME_UNKNOWN_TYPE", "FPP", null
        );
        UnknownCallback mapped = new UnknownCallback();
        when(webhookMapper.toUnknownCallback(RAW_BODY)).thenReturn(mapped);

        boolean result = webhookPersistenceService.save(RAW_BODY, payload);

        assertThat(result).isFalse();
        verify(unknownCallbackRepository).save(mapped);
        verify(paymentProviderCallbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("save — null тип → сохраняет в unknown_callbacks, возвращает false")
    void save_nullType_savesUnknownCallbackAndReturnsFalse() {
        WebhookPayload payload = new WebhookPayload(PROVIDER_TX_UID, null, "FPP", null);
        when(webhookMapper.toUnknownCallback(any())).thenReturn(new UnknownCallback());

        boolean result = webhookPersistenceService.save(RAW_BODY, payload);

        assertThat(result).isFalse();
        verify(unknownCallbackRepository).save(any(UnknownCallback.class));
        verify(paymentProviderCallbackRepository, never()).save(any());
    }
}