package com.bank.service;

import com.bank.domain.account.DepositAccount;
import com.bank.domain.account.DepositStatus;
import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionType;
import com.bank.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Симуляция начисления процентов по депозиту.
 * Начисление идёт РОВНО раз в месяц и оформляется отдельной транзакцией
 * (INTEREST_ACCRUAL) со своим автоматически сгенерированным id — точно
 * так же, как переводы между депозитами.
 */
public class InterestAccrualService {

    private final TransactionRepository transactionRepository;
    private final IdGeneratorService idGenerator;

    public InterestAccrualService(TransactionRepository transactionRepository, IdGeneratorService idGenerator) {
        this.transactionRepository = transactionRepository;
        this.idGenerator = idGenerator;
    }

    /** Начислить проценты за один месяц. */
    public Transaction accrueOneMonth(DepositAccount account) {
        if (account.getStatus() != DepositStatus.ACTIVE) {
            throw new IllegalStateException("Нельзя начислять проценты: депозит не активен (" + account.getStatus() + ")");
        }
        if (account.isFullyMatured()) {
            throw new IllegalStateException("Депозит уже полностью отработал свой срок (" +
                    account.getTermMonths() + " мес.)");
        }
        BigDecimal interest = account.calculateMonthlyInterest();
        account.applyMonthlyAccrual(interest);

        Transaction transaction = new Transaction(
                idGenerator.nextTransactionId(), account.getAccountId(), TransactionType.INTEREST_ACCRUAL,
                interest, account.getCurrency(), LocalDateTime.now(),
                "Начисление процентов за месяц " + account.getMonthsAccrued() + " из " + account.getTermMonths());
        transactionRepository.save(transaction);
        account.addTransaction(transaction);
        return transaction;
    }

    /** Прогнать симуляцию на N месяцев вперёд (не больше оставшегося срока депозита). */
    public List<Transaction> simulateMonths(DepositAccount account, int months) {
        if (months < 0) {
            throw new IllegalArgumentException("Количество месяцев не может быть отрицательным");
        }
        int monthsToAccrue = Math.min(months, account.getRemainingMonths());
        List<Transaction> result = new ArrayList<>();
        for (int i = 0; i < monthsToAccrue; i++) {
            result.add(accrueOneMonth(account));
        }
        return result;
    }

    /**
     * Прогнать симуляцию до конкретной даты — считает, сколько полных
     * месяцев прошло с момента последнего начисления, и начисляет
     * проценты соответствующее число раз.
     */
    public List<Transaction> simulateToDate(DepositAccount account, LocalDate targetDate) {
        LocalDate lastAccrualPoint = account.getOpenDate().plusMonths(account.getMonthsAccrued());
        long fullMonths = ChronoUnit.MONTHS.between(lastAccrualPoint, targetDate);
        if (fullMonths <= 0) {
            return new ArrayList<>();
        }
        return simulateMonths(account, (int) fullMonths);
    }
}
