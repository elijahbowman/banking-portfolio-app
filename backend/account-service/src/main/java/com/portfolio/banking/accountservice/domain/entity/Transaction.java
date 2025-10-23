package com.portfolio.banking.accountservice.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
@Data
@NoArgsConstructor
public class Transaction {
    @Id
    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "from_account_id") // Nullable for DEPOSIT/WITHDRAWAL
    private String fromAccountId;

    @Column(name = "to_account_id") // Nullable for DEPOSIT/WITHDRAWAL
    private String toAccountId;

    @Column(name = "account_id") // Used for DEPOSIT/WITHDRAWAL
    private String accountId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType; // TRANSFER, DEPOSIT, WITHDRAWAL

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}