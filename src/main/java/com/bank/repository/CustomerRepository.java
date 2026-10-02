package com.bank.repository;

import com.bank.domain.customer.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Абстракция хранилища клиентов ("таблица" в терминах требований задания).
 * Сейчас реализована in-memory ({@code InMemoryCustomerRepository}), но
 * интерфейс позволяет в будущем подключить настоящую БД (JDBC/JPA/Mongo),
 * не меняя ни сервисный, ни доменный слой — Dependency Inversion Principle.
 */
public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(long id);
    List<Customer> findAll();
}
