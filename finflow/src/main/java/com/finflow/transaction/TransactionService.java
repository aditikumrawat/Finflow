package com.finflow.transaction;

import com.finflow.exception.IdempotencyKeyReuseException;
import com.finflow.exception.InsufficientBalanceException;
import com.finflow.exception.InvalidTransferException;
import com.finflow.exception.WalletNotFoundException;
import com.finflow.transaction.dto.TransactionResponse;
import com.finflow.transaction.dto.TransferRequest;
import com.finflow.transaction.util.RequestFingerprint;
import com.finflow.wallet.Wallet;
import com.finflow.wallet.WalletRepository;
import com.finflow.wallet.WalletStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public TransactionResponse transfer(
            String senderEmail,
            TransferRequest request
    ) {

        Optional<Transaction> existingTransaction =
                transactionRepository.findByReferenceId(
                        request.referenceId()
                );

        Wallet senderWallet = walletRepository
                .findByUserEmail(senderEmail)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Sender wallet not found"
                        )
                );

        String fingerprint =
                RequestFingerprint.generate(
                        senderWallet.getId().toString(),
                        request.receiverWalletId().toString(),
                        request.amount().toPlainString(),
                        "INR",
                        request.referenceId()
                );

        if (existingTransaction.isPresent()) {

            Transaction existing = existingTransaction.get();

            if (!fingerprint.equals(existing.getRequestFingerprint())) {
                throw new IdempotencyKeyReuseException(
                        "Reference ID has already been used for a different transaction"
                );
            }

            return toResponse(existing);
        }

        Wallet receiverWallet = walletRepository
                .findById(request.receiverWalletId())
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Receiver wallet not found"
                        )
                );

        if (senderWallet.getId()
                .equals(receiverWallet.getId())) {

            throw new InvalidTransferException(
                    "Sender and receiver wallets must be different"
            );
        }

        if (!WalletStatus.ACTIVE.equals(senderWallet.getStatus())) {
            throw new InvalidTransferException(
                    "Sender wallet is not active"
            );
        }

        if (!WalletStatus.ACTIVE.equals(receiverWallet.getStatus())) {
            throw new InvalidTransferException(
                    "Receiver wallet is not active"
            );
        }

        if (!senderWallet.getCurrency()
                .equals(receiverWallet.getCurrency())) {

            throw new InvalidTransferException(
                    "Wallet currencies do not match"
            );
        }

        if (senderWallet.getBalance()
                .compareTo(request.amount()) < 0) {

            throw new InsufficientBalanceException();
        }

        Transaction transaction = new Transaction();

        transaction.setSenderWallet(senderWallet);
        transaction.setReceiverWallet(receiverWallet);
        transaction.setAmount(request.amount());
        transaction.setCurrency(senderWallet.getCurrency());
        transaction.setReferenceId(request.referenceId());
        transaction.setDescription(request.description());
        transaction.setRequestFingerprint(fingerprint);
        transaction.setStatus(TransactionStatus.PENDING);

        transaction = transactionRepository.save(transaction);

        senderWallet.setBalance(
                senderWallet.getBalance()
                        .subtract(request.amount())
        );

        receiverWallet.setBalance(
                receiverWallet.getBalance()
                        .add(request.amount())
        );

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setCompletedAt(
                java.time.LocalDateTime.now()
        );

        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getMyTransactions(
            String email,
            Pageable pageable
    ) {

        Wallet wallet = walletRepository
                .findByUserEmail(email)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found"
                        )
                );

        return transactionRepository
                .findBySenderWalletIdOrReceiverWalletId(
                        wallet.getId(),
                        wallet.getId(),
                        pageable
                )
                .map(this::toResponse);
    }

        private TransactionResponse toResponse(
            Transaction transaction
    ) {

        return new TransactionResponse(
                transaction.getId(),
                transaction.getSenderWallet().getId(),
                transaction.getReceiverWallet().getId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getReferenceId(),
                transaction.getDescription(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt()
        );
    }

}