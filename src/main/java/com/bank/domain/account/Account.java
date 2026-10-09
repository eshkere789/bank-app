package com.bank.domain.account;

import com.bank.domain.currency.Currency;
import com.bank.domain.transaction.Transaction;
import com.bank.exception.AccountBlockedException;
import com.bank.exception.InsufficientFundsException;
import com.bank.exception.InvalidAmountException;
import com.bank.exception.WithdrawalNotAllowedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Базовый банковский счёт.
 *
 * Инкапсулирует баланс и историю транзакций. Бизнес-правила пополнения/снятия
 * реализованы ЗДЕСЬ, а не в сервисном слое — это "богатая" доменная модель
 * (rich domain model): объект сам следит за тем, чтобы никогда не оказаться
 * в невалидном состоянии (отрицательный баланс, снятие с запрещённого счёта).
 * Сервисный слой (AccountService) отвечает только за оркестрацию: генерацию id,
 * персистентность и создание записи транзакции.
 */
public abstract class Account {

    protected final long accountId;
    protected final String accountNumber;
    protected final long customerId;
    protected final Currency currency;
    protected BigDecimal balance;
    protected final LocalDateTime createdAt;
    protected final List<Transaction> transactions = new ArrayList<>();
    private boolean blocked = false;

    protected Account(long accountId, String accountNumber, long customerId,
                      Currency currency, BigDecimal initialBalance) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.currency = Objects.requireNonNull(currency, "currency");
        // Требование: BigDecimal.ZERO, если баланс не передан явно
        this.balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    public abstract AccountType getAccountType();

    public abstract boolean isWithdrawalAllowed();

    public long getAccountId() {
        return accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public long getCustomerId() {
        return customerId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    /** Пополнение разрешено для ЛЮБОГО типа депозита. */
    public void deposit(BigDecimal amount) {
        ensureNotBlocked();
        validatePositive(amount);
        this.balance = this.balance.add(amount);
    }

    /** Снятие разрешено только если {@link #isWithdrawalAllowed()} возвращает true. */
    public void withdraw(BigDecimal amount) {
        ensureNotBlocked();
        if (!isWithdrawalAllowed()) {
            throw new WithdrawalNotAllowedException(
                    "Снятие средств запрещено для счёта " + accountNumber + " (тип: " + getAccountType() + ")");
        }
        validatePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Недостаточно средств на счёте " + accountNumber + ": баланс=" + balance + ", запрошено=" + amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void block() {
        this.blocked = true;
    }

    /**
     * Принудительное списание (взыскание по кредиту): забирает не больше maxAmount
     * и не больше текущего баланса, игнорируя запрет на снятие и блокировку.
     * Возвращает фактически списанную сумму.
     */
    public BigDecimal seize(BigDecimal maxAmount) {
        BigDecimal taken = balance.min(maxAmount);
        if (taken.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        this.balance = this.balance.subtract(taken);
        return taken;
    }

    protected void ensureNotBlocked() {
        if (blocked) {
            throw new AccountBlockedException("Счёт " + accountNumber + " заблокирован");
        }
    }

    protected void validatePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Сумма операции должна быть положительной: " + amount);
        }
    }
}
