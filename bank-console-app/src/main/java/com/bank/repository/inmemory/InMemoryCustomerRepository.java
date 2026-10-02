package com.bank.repository.inmemory;

import com.bank.domain.customer.Customer;
import com.bank.repository.CustomerRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** "Таблица" клиентов в оперативной памяти (аналог таблицы customers в реальной БД). */
public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<Long, Customer> table = new ConcurrentHashMap<>();

    @Override
    public Customer save(Customer customer) {
        table.put(customer.getId(), customer);
        return customer;
    }

    @Override
    public Optional<Customer> findById(long id) {
        return Optional.ofNullable(table.get(id));
    }

    @Override
    public List<Customer> findAll() {
        return List.copyOf(table.values());
    }
}
