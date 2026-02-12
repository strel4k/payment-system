package com.example.transaction.repository;

import com.example.transaction.entity.Transaction;
import com.example.transaction.entity.enums.PaymentType;
import com.example.transaction.entity.enums.TransactionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;


public final class TransactionSpecification {

    private TransactionSpecification() {

    }

    public static Specification<Transaction> hasUserUid(UUID userUid) {
        return (root, query, cb) ->
                userUid == null ? null : cb.equal(root.get("userUid"), userUid);
    }

    public static Specification<Transaction> hasWalletUid(UUID walletUid) {
        return (root, query, cb) ->
                walletUid == null ? null : cb.equal(root.get("wallet").get("uid"), walletUid);
    }

    public static Specification<Transaction> hasType(PaymentType type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> hasStatus(TransactionStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Transaction> createdAfter(LocalDateTime dateFrom) {
        return (root, query, cb) ->
                dateFrom == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom);
    }

    public static Specification<Transaction> createdBefore(LocalDateTime dateTo) {
        return (root, query, cb) ->
                dateTo == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), dateTo);
    }

    public static Specification<Transaction> buildSpecification(
            UUID userUid,
            UUID walletUid,
            PaymentType type,
            TransactionStatus status,
            LocalDateTime dateFrom,
            LocalDateTime dateTo) {

        return Specification.where(hasUserUid(userUid))
                .and(hasWalletUid(walletUid))
                .and(hasType(type))
                .and(hasStatus(status))
                .and(createdAfter(dateFrom))
                .and(createdBefore(dateTo));
    }
}