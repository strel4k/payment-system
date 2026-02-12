package com.example.transaction.kafka;

import com.example.transaction.entity.Transaction;
import com.example.transaction.entity.Wallet;
import com.example.transaction.kafka.event.DepositRequestedEvent;
import com.example.transaction.kafka.event.WithdrawalRequestedEvent;
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
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.deposit-requested}")
    private String depositRequestedTopic;

    @Value("${app.kafka.topics.withdrawal-requested}")
    private String withdrawalRequestedTopic;

    public void sendDepositRequested(Transaction transaction, Wallet wallet) {
        DepositRequestedEvent event = DepositRequestedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .transactionUid(transaction.getUid())
                .userUid(transaction.getUserUid())
                .walletUid(wallet.getUid())
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .currencyCode(wallet.getWalletType().getCurrencyCode())
                .paymentMethodId(transaction.getPaymentMethodId())
                .build();

        sendEvent(depositRequestedTopic, transaction.getUid().toString(), event);
    }

    public void sendWithdrawalRequested(Transaction transaction, Wallet wallet) {
        WithdrawalRequestedEvent event = WithdrawalRequestedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .transactionUid(transaction.getUid())
                .userUid(transaction.getUserUid())
                .walletUid(wallet.getUid())
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .totalAmount(transaction.getTotalAmount())
                .currencyCode(wallet.getWalletType().getCurrencyCode())
                .paymentMethodId(transaction.getPaymentMethodId())
                .build();

        sendEvent(withdrawalRequestedTopic, transaction.getUid().toString(), event);
    }

    private void sendEvent(String topic, String key, Object payload) {
        log.info("Sending event to topic {}: key={}, payload={}", topic, key, payload.getClass().getSimpleName());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event to topic {}: key={}", topic, key, ex);
            } else {
                log.info("Event sent to topic {}: key={}, partition={}, offset={}",
                        topic, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}