package com.bank.exception;

/** Нарушение правил кредитования (лимит банка, срок, счёт списания и т.д.). */
public class CreditException extends BankException {
    public CreditException(String message) {
        super(message);
    }
}
