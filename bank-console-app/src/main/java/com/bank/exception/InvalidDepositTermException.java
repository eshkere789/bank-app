package com.bank.exception;

public class InvalidDepositTermException extends BankException {
    public InvalidDepositTermException(String message) {
        super(message);
    }
}
