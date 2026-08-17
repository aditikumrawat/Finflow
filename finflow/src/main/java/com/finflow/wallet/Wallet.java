package com.finflow.wallet;

import com.finflow.entity.User;
import jakarta.persistence.*;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        if (currency == null) {
            currency = "INR";
        }

        if (status == null) {
            status = "ACTIVE";
        }
    }

    // getters and setters

    public void setBalance(BigDecimal zero) {
        this.balance = zero;
    }

    public void setUser(User savedUser) {
        this.user = savedUser;
    }

    public void setCurrency(String inr) {
        this.currency = inr;
    }

    public void setStatus(String active) {
        this.status = active;
    }

    public UUID getId() {
        return this.id;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public String getCurrency() {
        return this.currency;
    }

    public String getStatus() {
        return this.status;
    }
}