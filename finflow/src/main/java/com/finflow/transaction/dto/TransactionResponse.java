package com.finflow.transaction.dto;

import com.finflow.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID senderWalletId,
        UUID receiverWalletId,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        String referenceId,
        String description,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}