package com.bank.service;

import com.bank.domain.account.Account;
import com.bank.domain.account.AccumulativeDepositAccount;
import com.bank.domain.account.CurrentAccount;
import com.bank.domain.account.DepositAccount;
import com.bank.domain.account.DepositType;
import com.bank.domain.account.MultiCurrencyAccount;
import com.bank.domain.account.WithdrawableDepositAccount;
import com.bank.domain.currency.Currency;
import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionType;
import com.bank.exception.AccountNotFoundException;
import com.bank.rate.InterestRateSchedule;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Открытие депозитов и базовые операции (пополнение / снятие / досрочное
 * закрытие). Сервис отвечает за оркестрацию (генерация id, сохранение
 * в репозитории, запись транзакции), а бизнес-инварианты (можно ли снимать,
 * хватает ли денег) инкапсулированы в самих доменных классах Account/DepositAccount.
 */
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdGeneratorService idGenerator;

    public AccountService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           IdGeneratorService idGenerator) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idGenerator = idGenerator;
    }

    public CurrentAccount openCurrentAccount(long customerId, BigDecimal initialBalance) {
        CurrentAccount account = new CurrentAccount(
                idGenerator.nextAccountId(), idGenerator.nextAccountNumber("CA"), customerId, initialBalance);
        return (CurrentAccount) accountRepository.save(account);
    }

    public MultiCurrencyAccount openMultiCurrencyAccount(long customerId, Currency currency,
                                                           BigDecimal initialBalance) {
        MultiCurrencyAccount account = new MultiCurrencyAccount(
                idGenerator.nextAccountId(), idGenerator.nextAccountNumber("MC"),
                customerId, currency, initialBalance);
        return (MultiCurrencyAccount) accountRepository.save(account);
    }

    /** Депозит в тенге. */
    public WithdrawableDepositAccount openWithdrawableDeposit(long customerId, BigDecimal initialBalance,
                                                                int termMonths, LocalDate openDate) {
        return openWithdrawableDeposit(customerId, initialBalance, termMonths, openDate, Currency.KZT);
    }

    public WithdrawableDepositAccount openWithdrawableDeposit(long customerId, BigDecimal initialBalance,
                                                                int termMonths, LocalDate openDate,
                                                                Currency currency) {
        BigDecimal rate = InterestRateSchedule.rateFor(DepositType.WITHDRAWABLE, termMonths);
        long accountId = idGenerator.nextAccountId();
        String accountNumber = idGenerator.nextAccountNumber("WD");
        WithdrawableDepositAccount account = new WithdrawableDepositAccount(
                accountId, accountNumber, customerId, currency, initialBalance, rate, termMonths, openDate);
        return (WithdrawableDepositAccount) accountRepository.save(account);
    }

    /** Депозит в тенге. */
    public AccumulativeDepositAccount openAccumulativeDeposit(long customerId, BigDecimal initialBalance,
                                                                 int termMonths, LocalDate openDate) {
        return openAccumulativeDeposit(customerId, initialBalance, termMonths, openDate, Currency.KZT);
    }

    public AccumulativeDepositAccount openAccumulativeDeposit(long customerId, BigDecimal initialBalance,
                                                                 int termMonths, LocalDate openDate,
                                                                 Currency currency) {
        BigDecimal rate = InterestRateSchedule.rateFor(DepositType.ACCUMULATIVE, termMonths);
        long accountId = idGenerator.nextAccountId();
        String accountNumber = idGenerator.nextAccountNumber("AC");
        AccumulativeDepositAccount account = new AccumulativeDepositAccount(
                accountId, accountNumber, customerId, currency, initialBalance, rate, termMonths, openDate);
        return (AccumulativeDepositAccount) accountRepository.save(account);
    }

    public Account getById(long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Счёт не найден: id=" + accountId));
    }

    public DepositAccount getDepositById(long accountId) {
        Account account = getById(accountId);
        if (!(account instanceof DepositAccount)) {
            throw new AccountNotFoundException("Счёт id=" + accountId + " не является депозитом");
        }
        return (DepositAccount) account;
    }

    public List<Account> getAccountsByCustomer(long customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    public Transaction deposit(long accountId, BigDecimal amount) {
        Account account = getById(accountId);
        account.deposit(amount);
        return recordTransaction(account, TransactionType.DEPOSIT, amount, "Пополнение счёта");
    }

    public Transaction withdraw(long accountId, BigDecimal amount) {
        Account account = getById(accountId);
        account.withdraw(amount);
        return recordTransaction(account, TransactionType.WITHDRAWAL, amount.negate(), "Снятие со счёта");
    }

    /** Досрочное закрытие: всё накопленное за счёт симуляций вознаграждение сгорает. */
    public DepositAccount closeDepositEarly(long accountId) {
        DepositAccount account = getDepositById(accountId);
        BigDecimal forfeited = account.forfeitAccruedInterestAndClose();
        if (forfeited.signum() > 0) {
            recordTransaction(account, TransactionType.INTEREST_FORFEITED, forfeited.negate(),
                    "Досрочное закрытие: списано накопленное вознаграждение");
        }
        return account;
    }

    public DepositAccount closeDepositAtMaturity(long accountId) {
        DepositAccount account = getDepositById(accountId);
        account.closeAtMaturity();
        return account;
    }

    private Transaction recordTransaction(Account account, TransactionType type,
                                           BigDecimal amount, String description) {
        Transaction transaction = new Transaction(
                idGenerator.nextTransactionId(), account.getAccountId(), type,
                amount, account.getCurrency(), LocalDateTime.now(), description);
        transactionRepository.save(transaction);
        account.addTransaction(transaction);
        return transaction;
    }
}
