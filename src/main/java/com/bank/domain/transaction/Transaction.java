package com.bank.domain.transaction;

import com.bank.domain.currency.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Транзакция — неизменяемая запись о движении средств по счёту.
 * КАЖДАЯ транзакция (включая ежемесячное начисление процентов
 * и переводы между депозитами) получает собственный уникальный id,
 * сгенерированный тем же генератором, что и id счетов/клиентов.
 */
public final class Transaction {

    private final long transactionId;
    private final long accountId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final Currency currency;
    private final LocalDateTime timestamp;
    private final String description;

    public Transaction(long transactionId, long accountId, TransactionType type,
                        BigDecimal amount, Currency currency, LocalDateTime timestamp, String description) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
        this.description = description;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public long getAccountId() {
        return accountId;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("#%d [%s] %s%s %s %s (%s)",
                transactionId, timestamp, amount.signum() >= 0 ? "+" : "", amount.setScale(2, RoundingMode.HALF_UP), currency, type, description);
    }
}
