package com.bank.exception;

public class InvalidCurrencyException extends BankException {
    public InvalidCurrencyException(String message) {
        super(message);
    }
}
