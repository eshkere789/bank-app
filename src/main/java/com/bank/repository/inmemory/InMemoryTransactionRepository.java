package com.bank.repository.inmemory;

import com.bank.domain.transaction.Transaction;
import com.bank.repository.TransactionRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** "Таблица" транзакций в оперативной памяти. */
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<Long, Transaction> table = new ConcurrentHashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        table.put(transaction.getTransactionId(), transaction);
        return transaction;
    }

    @Override
    public List<Transaction> findByAccountId(long accountId) {
        return table.values().stream()
                .filter(t -> t.getAccountId() == accountId)
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findAll() {
        return List.copyOf(table.values());
    }
}
