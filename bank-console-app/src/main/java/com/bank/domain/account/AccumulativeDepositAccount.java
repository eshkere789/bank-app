package com.bank.domain.account;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Накопительный депозит: только пополнение, без права снятия до конца срока.
 * За отказ клиента от гибкости банк предлагает более высокую ставку.
 */
public final class AccumulativeDepositAccount extends DepositAccount {

    public AccumulativeDepositAccount(long accountId, String accountNumber, long customerId,
                                       BigDecimal initialBalance, BigDecimal interestRate,
                                       int termMonths, LocalDate openDate) {
        super(accountId, accountNumber, customerId, initialBalance, interestRate, termMonths, openDate);
    }

    @Override
    public DepositType getDepositType() {
        return DepositType.ACCUMULATIVE;
    }

    @Override
    public boolean isWithdrawalAllowed() {
        return false;
    }
}
