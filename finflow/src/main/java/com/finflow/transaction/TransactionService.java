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
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final IdempotencyService idempotencyService;

    public TransactionService(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository, IdempotencyService idempotencyService
    ) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public TransferResult transfer(
            String senderEmail,
            TransferRequest request
    ) {

        Wallet senderLookup = walletRepository
                .findByUserEmailForUpdate(senderEmail)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Sender wallet not found"
                        )
                );

        UUID senderWalletId = senderLookup.getId();
        UUID receiverWalletId = request.receiverWalletId();

        if (senderWalletId.equals(receiverWalletId)) {
            throw new InvalidTransferException(
                    "Sender and receiver wallets must be different"
            );
        }

        String fingerprint =
                RequestFingerprint.generate(
                        senderWalletId.toString(),
                        receiverWalletId.toString(),
                        request.amount().toPlainString(),
                        "INR",
                        request.referenceId()
                );

        Optional<Transaction> existingTransaction =
                transactionRepository.findByReferenceId(
                        request.referenceId()
                );

        if (existingTransaction.isPresent()) {

            Transaction existing = existingTransaction.get();

            if (!fingerprint.equals(existing.getRequestFingerprint())) {
                throw new IdempotencyKeyReuseException(
                        "Reference ID has already been used for a different transaction"
                );
            }

            return new TransferResult(
                    toResponse(existing),
                    true
            );
        }

        UUID firstWalletId;
        UUID secondWalletId;

        if (senderWalletId.compareTo(receiverWalletId) < 0) {
            firstWalletId = senderWalletId;
            secondWalletId = receiverWalletId;
        } else {
            firstWalletId = receiverWalletId;
            secondWalletId = senderWalletId;
        }

        Wallet firstLockedWallet =
                walletRepository
                        .findByIdForUpdate(firstWalletId)
                        .orElseThrow(() ->
                                new WalletNotFoundException(
                                        "Wallet not found"
                                )
                        );

        Wallet secondLockedWallet =
                walletRepository
                        .findByIdForUpdate(secondWalletId)
                        .orElseThrow(() ->
                                new WalletNotFoundException(
                                        "Wallet not found"
                                )
                        );

        Wallet lockedSenderWallet;
        Wallet lockedReceiverWallet;

        if (firstLockedWallet.getId().equals(senderWalletId)) {
            lockedSenderWallet = firstLockedWallet;
            lockedReceiverWallet = secondLockedWallet;
        } else {
            lockedSenderWallet = secondLockedWallet;
            lockedReceiverWallet = firstLockedWallet;
        }

        if (!WalletStatus.ACTIVE.equals(lockedSenderWallet.getStatus())) {
            throw new InvalidTransferException(
                    "Sender wallet is not active"
            );
        }

        if (!WalletStatus.ACTIVE.equals(lockedReceiverWallet.getStatus())) {
            throw new InvalidTransferException(
                    "Receiver wallet is not active"
            );
        }

        if (!lockedSenderWallet.getCurrency().equals(lockedReceiverWallet.getCurrency())) {
            throw new InvalidTransferException(
                    "Wallet currencies do not match"
            );
        }

        if (lockedSenderWallet.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException();
        }

        Transaction transaction = new Transaction();
        transaction.setSenderWallet(lockedSenderWallet);
        transaction.setReceiverWallet(lockedReceiverWallet);
        transaction.setAmount(request.amount());
        transaction.setCurrency(lockedSenderWallet.getCurrency());
        transaction.setReferenceId(request.referenceId());
        transaction.setDescription(request.description());
        transaction.setRequestFingerprint(fingerprint);
        transaction.setStatus(TransactionStatus.PENDING);

        try {
            transactionRepository.saveAndFlush(transaction);
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {

            Transaction existing =
                    idempotencyService.getExistingTransaction(
                            request.referenceId()
                    );

            if (!fingerprint.equals(existing.getRequestFingerprint())) {
                throw new IdempotencyKeyReuseException(
                        "Reference ID has already been used for a different transaction"
                );
            }
            return new TransferResult(
                    toResponse(existing),
                    true
            );
        }

        lockedSenderWallet.setBalance(
                lockedSenderWallet.getBalance()
                        .subtract(request.amount())
        );

        lockedReceiverWallet.setBalance(
                lockedReceiverWallet.getBalance()
                        .add(request.amount())
        );

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setCompletedAt(java.time.LocalDateTime.now());

        return new TransferResult(
                toResponse(transaction),
                false
        );
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

    public record TransferResult(
            TransactionResponse response,
            boolean alreadyProcessed
    ) {
    }
}