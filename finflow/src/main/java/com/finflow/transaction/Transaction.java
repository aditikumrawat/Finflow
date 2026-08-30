package com.finflow.transaction;

import com.finflow.wallet.Wallet;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transaction_sender", columnList = "sender_wallet_id"),
                @Index(name = "idx_transaction_receiver", columnList = "receiver_wallet_id"),
                @Index(name = "idx_transaction_reference", columnList = "reference_id")
        }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_wallet_id", nullable = false)
    private Wallet senderWallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_wallet_id", nullable = false)
    private Wallet receiverWallet;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(
            name = "reference_id",
            nullable = false,
            unique = true,
            length = 100
    )
    private String referenceId;

    @Column(length = 255)
    private String description;

    @Column(
            name = "request_fingerprint"
    )
    private String requestFingerprint;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = TransactionStatus.PENDING;
        }

        if (currency == null) {
            currency = "INR";
        }
    }

    public void setSenderWallet(Wallet senderWallet) {
        this.senderWallet = senderWallet;
    }

    public void setReceiverWallet(Wallet receiverWallet) {
        this.receiverWallet = receiverWallet;
    }

    public void setAmount(@NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
        this.amount = amount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setReferenceId(@NotBlank String refId) {
        this.referenceId = refId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(TransactionStatus transactionStatus) {
        this.status = transactionStatus;
    }

    public void setCompletedAt(LocalDateTime now) {
        this.completedAt = now;
    }

    public void setRequestFingerprint(String fingerprint) {
        this.requestFingerprint = fingerprint;
    }

    public UUID getId() {
        return this.id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return this.currency;
    }

    public TransactionStatus getStatus() {
        return this.status;
    }

    public String getReferenceId() {
        return this.referenceId;
    }

    public String getDescription() {
        return this.description;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return this.completedAt;
    }

    public Wallet getSenderWallet() {
        return this.senderWallet;
    }

    public Wallet getReceiverWallet() {
        return this.receiverWallet;
    }

    public String getRequestFingerprint() {
        return this.requestFingerprint;
    }


}