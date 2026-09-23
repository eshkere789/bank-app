package com.bank.service;

import com.bank.domain.account.WithdrawableDepositAccount;
import com.bank.domain.transaction.Transaction;
import com.bank.repository.inmemory.InMemoryDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterestAccrualServiceTest {

    private AccountService accountService;
    private InterestAccrualService interestAccrualService;

    @BeforeEach
    void setUp() {
        InMemoryDatabase db = new InMemoryDatabase();
        IdGeneratorService idGenerator = new IdGeneratorService();
        accountService = new AccountService(db.accounts(), db.transactions(), idGenerator);
        interestAccrualService = new InterestAccrualService(db.transactions(), idGenerator);
    }

    @Test
    void earlyClosureForfeitsAllAccruedInterest() {
        WithdrawableDepositAccount account = accountService.openWithdrawableDeposit(
                1L, new BigDecimal("100000"), 6, LocalDate.of(2026, 1, 1));

        interestAccrualService.simulateMonths(account, 2);
        BigDecimal accruedBeforeClosure = account.getAccruedInterestTotal();
        assertTrue(accruedBeforeClosure.signum() > 0);

        BigDecimal balanceBeforeClosure = account.getBalance();
        accountService.closeDepositEarly(account.getAccountId());

        BigDecimal expectedBalanceAfterClosure = balanceBeforeClosure.subtract(accruedBeforeClosure);
        assertEquals(0, expectedBalanceAfterClosure.compareTo(account.getBalance()));
        assertEquals(0, account.getAccruedInterestTotal().signum());
    }

    @Test
    void simulateMonthsDoesNotExceedRemainingTerm() {
        WithdrawableDepositAccount account = accountService.openWithdrawableDeposit(
                1L, new BigDecimal("10000"), 3, LocalDate.of(2026, 1, 1));

        List<Transaction> accruals = interestAccrualService.simulateMonths(account, 10);
        assertEquals(3, accruals.size());
        assertTrue(account.isFullyMatured());
    }
}
