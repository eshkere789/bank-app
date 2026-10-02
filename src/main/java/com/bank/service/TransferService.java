package com.bank.service;

import com.bank.domain.account.Account;
import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionType;
import com.bank.exception.AccountBlockedException;
import com.bank.exception.InvalidAmountException;
import com.bank.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Перевод между депозитами. Технически реализован как пара
 * "снятие с источника + пополнение получателя": исходный счёт обязан
 * разрешать снятие (это проверяется внутри {@code Account.withdraw}),
 * а зачисление разрешено для депозита любого типа, поскольку пополнение
 * доступно всегда. Списание выполняется первым: если оно падает
 * с исключением — начисление получателю не произойдёт.
 */
public class TransferService {

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final IdGeneratorService idGenerator;

    public TransferService(AccountService accountService,
                            TransactionRepository transactionRepository,
                            IdGeneratorService idGenerator) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.idGenerator = idGenerator;
    }

    public void transfer(long fromAccountId, long toAccountId, BigDecimal amount) {
        if (fromAccountId == toAccountId) {
            throw new InvalidAmountException("Нельзя перевести депозит сам на себя");
        }
        Account from = accountService.getById(fromAccountId);
        Account to = accountService.getById(toAccountId);

        if (to.isBlocked()) {
            throw new AccountBlockedException("Счёт получателя " + to.getAccountNumber() + " заблокирован");
        }
        from.withdraw(amount);
        to.deposit(amount);

        record(from, TransactionType.TRANSFER_OUT, amount.negate(),
                "Перевод на счёт " + to.getAccountNumber());
        record(to, TransactionType.TRANSFER_IN, amount,
                "Перевод со счёта " + from.getAccountNumber());
    }

    private void record(Account account, TransactionType type, BigDecimal amount, String description) {
        Transaction transaction = new Transaction(
                idGenerator.nextTransactionId(), account.getAccountId(), type,
                amount, LocalDateTime.now(), description);
        transactionRepository.save(transaction);
        account.addTransaction(transaction);
    }
}
