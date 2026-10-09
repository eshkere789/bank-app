package com.bank.domain.account;

import com.bank.domain.currency.Currency;
import com.bank.exception.InvalidCurrencyException;

import java.math.BigDecimal;

/** Мультивалютный счёт: хранит деньги в долларах (USD) или евро (EUR). */
public final class MultiCurrencyAccount extends Account {

    public MultiCurrencyAccount(long accountId, String accountNumber, long customerId,
                                 Currency currency, BigDecimal initialBalance) {
        super(accountId, accountNumber, customerId, requireForeign(currency), initialBalance);
    }

    private static Currency requireForeign(Currency currency) {
        if (currency == null || currency == Currency.KZT) {
            throw new InvalidCurrencyException(
                    "Мультивалютный счёт открывается только в USD или EUR (тенге — это текущий счёт)");
        }
        return currency;
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.MULTI_CURRENCY;
    }

    @Override
    public boolean isWithdrawalAllowed() {
        return true;
    }
}
