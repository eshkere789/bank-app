package com.bank.repository;

import com.bank.domain.transaction.Transaction;

import java.util.List;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    List<Transaction> findByAccountId(long accountId);
    List<Transaction> findAll();
}
