package com.example.transaction.repository;

import com.example.transaction.entity.Transaction;
import com.example.transaction.entity.enums.PaymentType;
import com.example.transaction.entity.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByUserUid(UUID userUid);

    Page<Transaction> findByUserUid(UUID userUid, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.wallet.uid = :walletUid")
    List<Transaction> findByWalletUid(@Param("walletUid") UUID walletUid);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.wallet WHERE t.uid = :uid")
    Optional<Transaction> findByIdWithWallet(@Param("uid") UUID uid);

    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.wallet w " +
            "LEFT JOIN FETCH t.targetWallet tw " +
            "WHERE t.uid = :uid")
    Optional<Transaction> findByIdWithWallets(@Param("uid") UUID uid);

    List<Transaction> findByTypeAndStatus(PaymentType type, TransactionStatus status);

    Page<Transaction> findByUserUidAndType(UUID userUid, PaymentType type, Pageable pageable);

    Page<Transaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    long countByUserUidAndStatus(UUID userUid, TransactionStatus status);
}