package com.finflow.exception;

public class InsufficientBalanceException
        extends RuntimeException {

    public InsufficientBalanceException() {
        super("Insufficient wallet balance");
    }
}