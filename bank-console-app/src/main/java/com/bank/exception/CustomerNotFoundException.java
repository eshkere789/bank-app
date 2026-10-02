package com.bank.exception;

public class CustomerNotFoundException extends BankException {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}
