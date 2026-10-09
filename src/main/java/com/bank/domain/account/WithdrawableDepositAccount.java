package com.bank.domain.account;

import com.bank.domain.currency.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Депозит с пополнением И снятием ("гибкий" депозит).
 * За гибкость банк платит более низкой ставкой (см. InterestRateSchedule).
 */
public final class WithdrawableDepositAccount extends DepositAccount {

    public WithdrawableDepositAccount(long accountId, String accountNumber, long customerId, Currency currency,
                                       BigDecimal initialBalance, BigDecimal interestRate,
                                       int termMonths, LocalDate openDate) {
        super(accountId, accountNumber, customerId, currency, initialBalance, interestRate, termMonths, openDate);
    }

    @Override
    public DepositType getDepositType() {
        return DepositType.WITHDRAWABLE;
    }

    @Override
    public boolean isWithdrawalAllowed() {
        return true;
    }
}
