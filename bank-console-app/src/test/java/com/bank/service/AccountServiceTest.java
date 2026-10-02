package com.bank.service;

import com.bank.domain.account.AccumulativeDepositAccount;
import com.bank.domain.account.WithdrawableDepositAccount;
import com.bank.exception.WithdrawalNotAllowedException;
import com.bank.repository.inmemory.InMemoryDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountServiceTest {

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        InMemoryDatabase db = new InMemoryDatabase();
        IdGeneratorService idGenerator = new IdGeneratorService();
        accountService = new AccountService(db.accounts(), db.transactions(), idGenerator);
    }

    @Test
    void newDepositDefaultsToZeroBalanceWhenNotProvided() {
        WithdrawableDepositAccount account =
                accountService.openWithdrawableDeposit(1L, null, 6, LocalDate.of(2026, 1, 1));
        assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void endDateIsAutoCalculatedFromTerm() {
        WithdrawableDepositAccount account =
                accountService.openWithdrawableDeposit(1L, BigDecimal.TEN, 6, LocalDate.of(2026, 1, 1));
        assertEquals(LocalDate.of(2026, 7, 1), account.getEndDate());
    }

    @Test
    void withdrawalIsRejectedForAccumulativeDeposit() {
        AccumulativeDepositAccount account = accountService.openAccumulativeDeposit(
                1L, new BigDecimal("1000"), 3, LocalDate.of(2026, 1, 1));
        assertThrows(WithdrawalNotAllowedException.class,
                () -> accountService.withdraw(account.getAccountId(), BigDecimal.TEN));
    }
}
