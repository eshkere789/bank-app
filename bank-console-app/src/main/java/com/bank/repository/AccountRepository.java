package com.bank.repository;

import com.bank.domain.account.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(long id);
    List<Account> findByCustomerId(long customerId);
    List<Account> findAll();
}
