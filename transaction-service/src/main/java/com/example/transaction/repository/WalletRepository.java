package com.example.transaction.repository;

import com.example.transaction.entity.Wallet;
import com.example.transaction.entity.enums.WalletStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findByUserUid(UUID userUid);

    List<Wallet> findByUserUidAndStatus(UUID userUid, WalletStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.uid = :uid")
    Optional<Wallet> findByIdForUpdate(@Param("uid") UUID uid);

    Optional<Wallet> findByUidAndUserUid(UUID uid, UUID userUid);

    boolean existsByUserUidAndWalletTypeUid(UUID userUid, UUID walletTypeUid);

    @Query("SELECT w FROM Wallet w JOIN FETCH w.walletType WHERE w.uid = :uid")
    Optional<Wallet> findByIdWithWalletType(@Param("uid") UUID uid);
}