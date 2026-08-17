package com.finflow.wallet;

import com.finflow.wallet.dto.WalletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                walletService.getMyWallet(
                        authentication.getName()
                )
        );
    }
}