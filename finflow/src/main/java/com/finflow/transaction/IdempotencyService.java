package com.finflow.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

    private final TransactionRepository transactionRepository;

    public IdempotencyService(
            TransactionRepository transactionRepository
    ) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            readOnly = true
    )
    public Transaction getExistingTransaction(
            String referenceId
    ) {

        return transactionRepository
                .findByReferenceId(referenceId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Transaction not found after duplicate reference"
                        )
                );
    }
}