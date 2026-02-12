package com.example.transaction.kafka;

import com.example.transaction.entity.Transaction;
import com.example.transaction.entity.Wallet;
import com.example.transaction.entity.enums.TransactionStatus;
import com.example.transaction.kafka.event.DepositCompletedEvent;
import com.example.transaction.kafka.event.WithdrawalCompletedEvent;
import com.example.transaction.kafka.event.WithdrawalFailedEvent;
import com.example.transaction.repository.TransactionRepository;
import com.example.transaction.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.deposit-completed}",
            containerFactory = "depositCompletedListenerFactory"
    )
    @Transactional
    public void handleDepositCompleted(DepositCompletedEvent event) {
        log.info("Received DepositCompletedEvent: transactionUid={}, amount={}",
                event.getTransactionUid(), event.getAmount());

        try {
            Transaction transaction = transactionRepository.findById(event.getTransactionUid())
                    .orElseThrow(() -> new IllegalStateException(
                            "Transaction not found: " + event.getTransactionUid()));

            if (transaction.getStatus() != TransactionStatus.PENDING) {
                log.warn("Transaction {} is not in PENDING state, current state: {}",
                        event.getTransactionUid(), transaction.getStatus());
                return;
            }

            Wallet wallet = walletRepository.findByIdForUpdate(event.getWalletUid())
                    .orElseThrow(() -> new IllegalStateException(
                            "Wallet not found: " + event.getWalletUid()));

            wallet.credit(event.getAmount());
            walletRepository.save(wallet);

            transaction.complete();
            transactionRepository.save(transaction);

            log.info("Deposit completed: transactionUid={}, walletUid={}, amount={}, newBalance={}",
                    transaction.getUid(), wallet.getUid(), event.getAmount(), wallet.getBalance());

        } catch (Exception e) {
            log.error("Failed to process DepositCompletedEvent: transactionUid={}",
                    event.getTransactionUid(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.withdrawal-completed}",
            containerFactory = "withdrawalCompletedListenerFactory"
    )
    @Transactional
    public void handleWithdrawalCompleted(WithdrawalCompletedEvent event) {
        log.info("Received WithdrawalCompletedEvent: transactionUid={}, amount={}",
                event.getTransactionUid(), event.getAmount());

        try {
            Transaction transaction = transactionRepository.findById(event.getTransactionUid())
                    .orElseThrow(() -> new IllegalStateException(
                            "Transaction not found: " + event.getTransactionUid()));

            if (transaction.getStatus() != TransactionStatus.PENDING) {
                log.warn("Transaction {} is not in PENDING state, current state: {}",
                        event.getTransactionUid(), transaction.getStatus());
                return;
            }

            transaction.complete();
            transactionRepository.save(transaction);

            log.info("Withdrawal completed: transactionUid={}", transaction.getUid());

        } catch (Exception e) {
            log.error("Failed to process WithdrawalCompletedEvent: transactionUid={}",
                    event.getTransactionUid(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.withdrawal-failed}",
            containerFactory = "withdrawalFailedListenerFactory"
    )
    @Transactional
    public void handleWithdrawalFailed(WithdrawalFailedEvent event) {
        log.info("Received WithdrawalFailedEvent: transactionUid={}, reason={}",
                event.getTransactionUid(), event.getReason());

        try {
            Transaction transaction = transactionRepository.findById(event.getTransactionUid())
                    .orElseThrow(() -> new IllegalStateException(
                            "Transaction not found: " + event.getTransactionUid()));

            if (transaction.getStatus() != TransactionStatus.PENDING) {
                log.warn("Transaction {} is not in PENDING state, current state: {}",
                        event.getTransactionUid(), transaction.getStatus());
                return;
            }

            Wallet wallet = walletRepository.findByIdForUpdate(event.getWalletUid())
                    .orElseThrow(() -> new IllegalStateException(
                            "Wallet not found: " + event.getWalletUid()));

            wallet.credit(event.getRefundAmount());
            walletRepository.save(wallet);

            transaction.fail(event.getReason());
            transactionRepository.save(transaction);

            log.info("Withdrawal failed and refunded: transactionUid={}, walletUid={}, refundAmount={}, reason={}",
                    transaction.getUid(), wallet.getUid(), event.getRefundAmount(), event.getReason());

        } catch (Exception e) {
            log.error("Failed to process WithdrawalFailedEvent: transactionUid={}",
                    event.getTransactionUid(), e);
            throw e;
        }
    }
}
