package com.finflow.transaction.util;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class RequestFingerprint {
    private RequestFingerprint() {
    }

    public static String generate(
            String senderWalletId,
            String receiverWalletId,
            String amount,
            String currency,
            String referenceId
    ) {

        String rawValue = String.join(
                "|",
                senderWalletId,
                receiverWalletId,
                amount,
                currency,
                referenceId
        );

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            rawValue.getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm not available",
                    exception
            );
        }
    }
}
