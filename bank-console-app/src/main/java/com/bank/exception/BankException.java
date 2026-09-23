package com.bank.exception;

/** Базовое (unchecked) исключение всех бизнес-ошибок банковского домена. */
public class BankException extends RuntimeException {
    public BankException(String message) {
        super(message);
    }
}
