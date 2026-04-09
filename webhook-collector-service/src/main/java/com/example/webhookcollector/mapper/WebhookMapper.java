package com.example.webhookcollector.mapper;

import com.example.webhookcollector.controller.dto.WebhookPayload;
import com.example.webhookcollector.entity.PaymentProviderCallback;
import com.example.webhookcollector.entity.UnknownCallback;
import org.springframework.stereotype.Component;

@Component
public class WebhookMapper {

    public PaymentProviderCallback toPaymentProviderCallback(String rawBody, WebhookPayload payload) {
        PaymentProviderCallback callback = new PaymentProviderCallback();
        callback.setBody(rawBody);
        callback.setProviderTransactionUid(payload.providerTransactionUid());
        callback.setType(payload.type());
        callback.setProvider(payload.provider());
        return callback;
    }

    public UnknownCallback toUnknownCallback(String rawBody) {
        UnknownCallback callback = new UnknownCallback();
        callback.setBody(rawBody);
        return callback;
    }
}