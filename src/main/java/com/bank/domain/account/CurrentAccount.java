package com.bank.domain.account;

import com.bank.domain.currency.Currency;

import java.math.BigDecimal;

/** Обычный текущий счёт: всегда в тенге, пополнение и снятие без ограничений, процентов нет. */
public final class CurrentAccount extends Account {

    public CurrentAccount(long accountId, String accountNumber, long customerId, BigDecimal initialBalance) {
        super(accountId, accountNumber, customerId, Currency.KZT, initialBalance);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.CURRENT;
    }

    @Override
    public boolean isWithdrawalAllowed() {
        return true;
    }
}
