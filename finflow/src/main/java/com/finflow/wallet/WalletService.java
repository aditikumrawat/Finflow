package com.finflow.wallet;

import com.finflow.wallet.dto.WalletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public WalletResponse getMyWallet(String email) {

        Wallet wallet = walletRepository
                .findByUserEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Wallet not found")
                );

        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus()
        );
    }
}