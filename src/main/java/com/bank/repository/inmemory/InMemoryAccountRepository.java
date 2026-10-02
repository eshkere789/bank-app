package com.bank.repository.inmemory;

import com.bank.domain.account.Account;
import com.bank.repository.AccountRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** "Таблица" счетов/депозитов в оперативной памяти. */
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<Long, Account> table = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        table.put(account.getAccountId(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(long id) {
        return Optional.ofNullable(table.get(id));
    }

    @Override
    public List<Account> findByCustomerId(long customerId) {
        return table.values().stream()
                .filter(a -> a.getCustomerId() == customerId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Account> findAll() {
        return List.copyOf(table.values());
    }
}
