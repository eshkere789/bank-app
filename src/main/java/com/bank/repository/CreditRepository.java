package com.bank.repository;

import com.bank.domain.credit.Credit;

import java.util.List;
import java.util.Optional;

public interface CreditRepository {
    Credit save(Credit credit);
    Optional<Credit> findById(long id);
    List<Credit> findByCustomerId(long customerId);
    List<Credit> findAll();
}
