package com.bank.service;

import com.bank.domain.customer.ContactType;
import com.bank.domain.customer.Customer;
import com.bank.exception.CustomerNotFoundException;
import com.bank.repository.CustomerRepository;

import java.util.List;

/** Регистрация и поиск клиентов. */
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final IdGeneratorService idGenerator;

    public CustomerService(CustomerRepository customerRepository, IdGeneratorService idGenerator) {
        this.customerRepository = customerRepository;
        this.idGenerator = idGenerator;
    }

    public Customer registerCustomer(String firstName, String lastName,
                                      ContactType contactType, String contactValue) {
        Customer customer = Customer.builder()
                .id(idGenerator.nextCustomerId())
                .firstName(firstName)
                .lastName(lastName)
                .uniqueContact(contactType, contactValue)
                .build();
        return customerRepository.save(customer);
    }

    public Customer getById(long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Клиент не найден: id=" + customerId));
    }

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }
}
