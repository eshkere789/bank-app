package com.bank.service;

import com.bank.domain.account.Account;
import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionType;
import com.bank.exception.AccountBlockedException;
import com.bank.exception.InvalidAmountException;
import com.bank.repository.TransactionRepository;
import com.bank.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Перевод между любыми счетами и депозитами (текущий, мультивалютный, депозит).
 *
 * Сумма перевода задаётся в валюте СЧЁТА-ИСТОЧНИКА. Получателю зачисляется
 * сумма, автоматически сконвертированная в валюту его счёта по курсам из
 * {@link ExchangeRateService} (тенге → доллар, евро → доллар и т.д.).
 *
 * Технически это пара "снятие с источника + пополнение получателя": источник
 * обязан разрешать снятие (проверяется внутри Account.withdraw), получатель
 * принимает пополнение в любом случае. Все проверки, которые могут упасть
 * на стороне получателя, выполняются ДО списания — иначе деньги исчезли бы.
 */
public class TransferService {

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final IdGeneratorService idGenerator;
    private final ExchangeRateService exchangeRateService;

    public TransferService(AccountService accountService,
                            TransactionRepository transactionRepository,
                            IdGeneratorService idGenerator,
                            ExchangeRateService exchangeRateService) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.idGenerator = idGenerator;
        this.exchangeRateService = exchangeRateService;
    }

    /** @return сумма, зачисленная получателю (в валюте его счёта) */
    public BigDecimal transfer(long fromAccountId, long toAccountId, BigDecimal amount) {
        if (fromAccountId == toAccountId) {
            throw new InvalidAmountException("Нельзя перевести деньги сам на себя");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Сумма операции должна быть положительной: " + amount);
        }
        Account from = accountService.getById(fromAccountId);
        Account to = accountService.getById(toAccountId);

        BigDecimal credited = exchangeRateService.convert(amount, from.getCurrency(), to.getCurrency());
        if (credited.signum() <= 0) {
            throw new InvalidAmountException("Сумма после конвертации слишком мала: "
                    + credited + " " + to.getCurrency());
        }
        if (to.isBlocked()) {
            throw new AccountBlockedException("Счёт получателя " + to.getAccountNumber() + " заблокирован");
        }

        from.withdraw(amount);
        to.deposit(credited);

        String conversion = from.getCurrency() == to.getCurrency() ? ""
                : " (конвертация " + MoneyUtils.normalize(amount) + " " + from.getCurrency()
                + " → " + MoneyUtils.normalize(credited) + " " + to.getCurrency() + ")";
        record(from, TransactionType.TRANSFER_OUT, amount.negate(),
                "Перевод на счёт " + to.getAccountNumber() + conversion);
        record(to, TransactionType.TRANSFER_IN, credited,
                "Перевод со счёта " + from.getAccountNumber() + conversion);
        return credited;
    }

    private void record(Account account, TransactionType type, BigDecimal amount, String description) {
        Transaction transaction = new Transaction(
                idGenerator.nextTransactionId(), account.getAccountId(), type,
                amount, account.getCurrency(), LocalDateTime.now(), description);
        transactionRepository.save(transaction);
        account.addTransaction(transaction);
    }
}
