package com.finflow.wallet.dto;

import com.finflow.wallet.WalletStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        BigDecimal balance,
        String currency,
        WalletStatus status
) {
}