package com.example.webhookcollector.service;

import com.example.webhookcollector.controller.dto.WebhookPayload;
import com.example.webhookcollector.entity.CallbackType;
import com.example.webhookcollector.mapper.WebhookMapper;
import com.example.webhookcollector.repository.PaymentProviderCallbackRepository;
import com.example.webhookcollector.repository.UnknownCallbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookPersistenceService {

    private final PaymentProviderCallbackRepository paymentProviderCallbackRepository;
    private final UnknownCallbackRepository unknownCallbackRepository;
    private final WebhookMapper webhookMapper;

    @Transactional
    public boolean save(String rawBody, WebhookPayload payload) {
        if (CallbackType.isKnown(payload.type())) {
            paymentProviderCallbackRepository.save(
                    webhookMapper.toPaymentProviderCallback(rawBody, payload)
            );
            log.debug("Saved payment_provider_callback: type={} providerTransactionUid={}",
                    payload.type(), payload.providerTransactionUid());
            return true;
        } else {
            unknownCallbackRepository.save(webhookMapper.toUnknownCallback(rawBody));
            log.warn("Unknown webhook type={}, saved to unknown_callbacks", payload.type());
            return false;
        }
    }
}