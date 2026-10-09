package com.bank.domain.account;

public enum AccountType {
    CURRENT,              // обычный текущий счёт в тенге
    MULTI_CURRENCY,       // мультивалютный счёт (USD / EUR)
    DEPOSIT_WITHDRAWABLE, // депозит с пополнением и снятием
    DEPOSIT_ACCUMULATIVE  // депозит только с пополнением
}
