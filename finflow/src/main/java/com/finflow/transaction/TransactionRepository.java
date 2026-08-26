package com.finflow.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

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
}