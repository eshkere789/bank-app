package com.bank.repository.inmemory;

import com.bank.repository.AccountRepository;
import com.bank.repository.CreditRepository;
import com.bank.repository.CustomerRepository;
import com.bank.repository.TransactionRepository;

/**
 * "Внутренняя БД" приложения: набор таблиц (customers, accounts, transactions),
 * живущих в оперативной памяти на время работы программы — как и требовалось
 * ("БД внутреннее, создаёте таблицы в самой структуре", без внешней СУБД).
 *
 * Это единая фасадная точка входа к хранилищу: чтобы в будущем подключить
 * настоящую СУБД, достаточно заменить реализации внутри этого класса —
 * доменный и сервисный слой не изменятся ни на строку.
 */
public class InMemoryDatabase {

    private final CustomerRepository customers = new InMemoryCustomerRepository();
    private final AccountRepository accounts = new InMemoryAccountRepository();
    private final TransactionRepository transactions = new InMemoryTransactionRepository();
    private final CreditRepository credits = new InMemoryCreditRepository();

    public CustomerRepository customers() {
        return customers;
    }

    public AccountRepository accounts() {
        return accounts;
    }

    public TransactionRepository transactions() {
        return transactions;
    }

    public CreditRepository credits() {
        return credits;
    }
}
