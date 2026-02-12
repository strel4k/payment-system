package com.example.transaction.entity;

import com.example.transaction.entity.enums.PaymentType;
import com.example.transaction.entity.enums.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;


@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Column(name = "user_uid", nullable = false)
    private UUID userUid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_uid", nullable = false)
    private Wallet wallet;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "comment", length = 256)
    private String comment;

    @Column(name = "fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_wallet_uid")
    private Wallet targetWallet;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "failure_reason", length = 256)
    private String failureReason;

    /**
     * Mark transaction as completed.
     */
    public void complete() {
        this.status = TransactionStatus.COMPLETED;
    }

    /**
     * Mark transaction as failed with reason.
     * @param reason failure reason
     */
    public void fail(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * Check if transaction is pending.
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return this.status == TransactionStatus.PENDING;
    }

    /**
     * Check if transaction is completed.
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return this.status == TransactionStatus.COMPLETED;
    }

    /**
     * Check if this is a transfer transaction.
     * @return true if type is TRANSFER
     */
    public boolean isTransfer() {
        return this.type == PaymentType.TRANSFER;
    }

    /**
     * Check if this is a deposit transaction.
     * @return true if type is DEPOSIT
     */
    public boolean isDeposit() {
        return this.type == PaymentType.DEPOSIT;
    }

    /**
     * Check if this is a withdrawal transaction.
     * @return true if type is WITHDRAWAL
     */
    public boolean isWithdrawal() {
        return this.type == PaymentType.WITHDRAWAL;
    }

    /**
     * Get total amount including fee.
     * For withdrawal: amount + fee (total deducted)
     * For deposit: amount (fee may be deducted from deposit)
     * @return total amount
     */
    public BigDecimal getTotalAmount() {
        return amount.add(fee != null ? fee : BigDecimal.ZERO);
    }
}