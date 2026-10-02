package com.bank.repository.inmemory;

import com.bank.domain.credit.Credit;
import com.bank.repository.CreditRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** "Таблица" кредитов в оперативной памяти. */
public class InMemoryCreditRepository implements CreditRepository {

    private final Map<Long, Credit> table = new ConcurrentHashMap<>();

    @Override
    public Credit save(Credit credit) {
        table.put(credit.getCreditId(), credit);
        return credit;
    }

    @Override
    public Optional<Credit> findById(long id) {
        return Optional.ofNullable(table.get(id));
    }

    @Override
    public List<Credit> findByCustomerId(long customerId) {
        return table.values().stream()
                .filter(c -> c.getCustomerId() == customerId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Credit> findAll() {
        return List.copyOf(table.values());
    }
}
