package com.portfolio.banking.bankingservice.domain.exception;

public class BalanceFetchException extends RuntimeException {
    public BalanceFetchException(String message) {
        super(message);
    }

    public BalanceFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}