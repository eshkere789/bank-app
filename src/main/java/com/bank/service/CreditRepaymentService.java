package com.bank.service;

import com.bank.domain.account.Account;
import com.bank.domain.credit.Credit;
import com.bank.domain.credit.CreditStatus;
import com.bank.domain.currency.Currency;
import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionType;
import com.bank.exception.CreditException;
import com.bank.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Симуляция погашения кредита по месяцам.
 *
 * Каждый месяц: если на счёте списания хватает денег — штатный платёж.
 * Если не хватает — симуляция НЕ останавливается, а запускает принудительное
 * взыскание: деньги забираются со всех счетов клиента (сначала со счёта
 * списания) в счёт долга. Если долг так и не покрыт — все счета клиента
 * блокируются, кредит получает статус DEFAULTED.
 */
public class CreditRepaymentService {

    private final CreditService creditService;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final IdGeneratorService idGenerator;
    private final ExchangeRateService exchangeRateService;

    public CreditRepaymentService(CreditService creditService, AccountService accountService,
                                  TransactionRepository transactionRepository,
                                  IdGeneratorService idGenerator,
                                  ExchangeRateService exchangeRateService) {
        this.creditService = creditService;
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.idGenerator = idGenerator;
        this.exchangeRateService = exchangeRateService;
    }

    public List<Transaction> simulateMonths(long creditId, int months) {
        if (months < 0) {
            throw new IllegalArgumentException("Количество месяцев не может быть отрицательным");
        }
        Credit credit = creditService.getById(creditId);
        if (credit.getStatus() != CreditStatus.ACTIVE) {
            throw new CreditException("Кредит не активен, статус: " + credit.getStatus());
        }

        List<Transaction> operations = new ArrayList<>();
        for (int i = 0; i < months && credit.getStatus() == CreditStatus.ACTIVE; i++) {
            BigDecimal due = credit.nextInstallmentAmount();
            Account repaymentAccount = accountService.getById(credit.getRepaymentAccountId());

            if (canPay(repaymentAccount, due)) {
                repaymentAccount.withdraw(due);
                operations.add(record(repaymentAccount, TransactionType.CREDIT_PAYMENT, due.negate(),
                        "Платёж по кредиту #" + creditId + ": " + (credit.getPaidInstallments() + 1)
                                + " из " + credit.getTermMonths()));
                credit.applyPaidInstallment();
            } else {
                operations.addAll(collectFromAllAccounts(credit, repaymentAccount));
            }
        }
        return operations;
    }

    private boolean canPay(Account account, BigDecimal due) {
        return !account.isBlocked()
                && account.isWithdrawalAllowed()
                && account.getBalance().compareTo(due) >= 0;
    }

    private List<Transaction> collectFromAllAccounts(Credit credit, Account repaymentAccount) {
        List<Transaction> operations = new ArrayList<>();
        BigDecimal debt = credit.debtForCollection();
        BigDecimal left = debt;

        List<Account> accounts = new ArrayList<>(accountService.getAccountsByCustomer(credit.getCustomerId()));
        accounts.sort(Comparator
                .comparing((Account a) -> a.getAccountId() != repaymentAccount.getAccountId())
                .thenComparing(Account::getAccountId));

        for (Account account : accounts) {
            if (left.signum() <= 0) {
                break;
            }
            // Долг в тенге, а счёт может быть в USD/EUR: сколько нужно списать в валюте счёта
            // (округляем вверх, чтобы покрыть долг без недобора в 1 тиын)
            Currency accountCurrency = account.getCurrency();
            BigDecimal neededInAccountCurrency =
                    exchangeRateService.convert(left, Currency.KZT, accountCurrency, RoundingMode.CEILING);
            BigDecimal taken = account.seize(neededInAccountCurrency);
            if (taken.signum() > 0) {
                BigDecimal takenInKzt = exchangeRateService.convert(taken, accountCurrency, Currency.KZT).min(left);
                left = left.subtract(takenInKzt);
                String conversion = accountCurrency == Currency.KZT ? ""
                        : " (" + taken + " " + accountCurrency + " ≈ " + takenInKzt + " KZT)";
                operations.add(record(account, TransactionType.CREDIT_SEIZURE, taken.negate(),
                        "Принудительное списание в счёт кредита #" + credit.getCreditId() + conversion));
            }
        }

        credit.applySeizure(debt.subtract(left), debt);
        if (credit.getStatus() == CreditStatus.DEFAULTED) {
            accounts.forEach(Account::block);
        }
        return operations;
    }

    private Transaction record(Account account, TransactionType type, BigDecimal amount, String description) {
        Transaction transaction = new Transaction(
                idGenerator.nextTransactionId(), account.getAccountId(), type,
                amount, account.getCurrency(), LocalDateTime.now(), description);
        transactionRepository.save(transaction);
        account.addTransaction(transaction);
        return transaction;
    }
}
