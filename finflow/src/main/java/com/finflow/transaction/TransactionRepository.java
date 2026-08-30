package com.finflow.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReferenceId(
            String referenceId
    );

    boolean existsByReferenceId(
            String referenceId
    );

    Page<Transaction> findBySenderWalletIdOrReceiverWalletId(
            UUID senderWalletId,
            UUID receiverWalletId,
            Pageable pageable
    );

    Optional<Transaction> findByIdAndSenderWalletIdOrIdAndReceiverWalletId(
            UUID id,
            UUID senderWalletId,
            UUID id2,
            UUID receiverWalletId
    );
}