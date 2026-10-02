package com.bank.service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Потокобезопасный генератор уникальных числовых идентификаторов
 * для клиентов, счетов и транзакций. ЛЮБАЯ транзакция — обычная операция,
 * перевод между депозитами или ежемесячное начисление процентов —
 * получает свой id через один и тот же счётчик transactionIds.
 */
public class IdGeneratorService {

    private final AtomicLong customerIds = new AtomicLong(1);
    private final AtomicLong accountIds = new AtomicLong(1);
    private final AtomicLong transactionIds = new AtomicLong(1);
    private final AtomicLong accountNumberSequence = new AtomicLong(1);

    public long nextCustomerId() {
        return customerIds.getAndIncrement();
    }

    public long nextAccountId() {
        return accountIds.getAndIncrement();
    }

    public long nextTransactionId() {
        return transactionIds.getAndIncrement();
    }

    /** Человекочитаемый номер счёта, например WD-000001 / AC-000002. */
    public String nextAccountNumber(String prefix) {
        return String.format("%s-%06d", prefix, accountNumberSequence.getAndIncrement());
    }
}
