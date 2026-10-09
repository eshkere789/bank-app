package com.bank.domain.account;

import com.bank.domain.currency.Currency;
import com.bank.exception.InvalidDepositTermException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Set;

/**
 * Срочный депозитный счёт: помимо баланса хранит процентную ставку,
 * срок в месяцах, дату открытия/окончания и статистику начислений.
 *
 * Дата окончания рассчитывается автоматически: openDate.plusMonths(termMonths).
 * Разрешены только сроки 3 / 6 / 9 / 12 месяцев (ALLOWED_TERMS_MONTHS).
 *
 * Это abstract-класс: конкретное поведение "можно ли снимать деньги"
 * определяют наследники {@link WithdrawableDepositAccount} и
 * {@link AccumulativeDepositAccount} — Open/Closed Principle: чтобы
 * добавить новый вид депозита в будущем, не нужно менять этот класс,
 * достаточно создать ещё одного наследника.
 */
public abstract class DepositAccount extends Account {

    /** Разрешённые сроки депозита в месяцах. */
    public static final Set<Integer> ALLOWED_TERMS_MONTHS = Set.of(3, 6, 9, 12);

    private final BigDecimal interestRate; // годовая ставка в %, напр. 17.5
    private final int termMonths;
    private final LocalDate openDate;
    private final LocalDate endDate;

    private int monthsAccrued = 0;
    private BigDecimal accruedInterestTotal = BigDecimal.ZERO;
    private DepositStatus status = DepositStatus.ACTIVE;

    protected DepositAccount(long accountId, String accountNumber, long customerId, Currency currency,
                              BigDecimal initialBalance, BigDecimal interestRate,
                              int termMonths, LocalDate openDate) {
        super(accountId, accountNumber, customerId, currency, initialBalance);
        if (!ALLOWED_TERMS_MONTHS.contains(termMonths)) {
            throw new InvalidDepositTermException(
                    "Недопустимый срок депозита: " + termMonths + " мес. Разрешено: " + ALLOWED_TERMS_MONTHS);
        }
        this.interestRate = interestRate;
        this.termMonths = termMonths;
        this.openDate = openDate;
        this.endDate = openDate.plusMonths(termMonths); // авто-расчёт даты окончания
    }

    /** Вид депозита (с правом снятия / без него) — определяют наследники. */
    public abstract DepositType getDepositType();

    @Override
    public AccountType getAccountType() {
        return getDepositType() == DepositType.WITHDRAWABLE
                ? AccountType.DEPOSIT_WITHDRAWABLE
                : AccountType.DEPOSIT_ACCUMULATIVE;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public LocalDate getOpenDate() {
        return openDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getMonthsAccrued() {
        return monthsAccrued;
    }

    public BigDecimal getAccruedInterestTotal() {
        return accruedInterestTotal;
    }

    public DepositStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == DepositStatus.ACTIVE;
    }

    public boolean isFullyMatured() {
        return monthsAccrued >= termMonths;
    }

    public int getRemainingMonths() {
        return Math.max(0, termMonths - monthsAccrued);
    }

    /**
     * Простой ежемесячный процент: balance * (годовая ставка / 100) / 12,
     * округление до 2 знаков (копейки), банковское округление HALF_UP.
     */
    public BigDecimal calculateMonthlyInterest() {
        return balance.multiply(interestRate)
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Применяет начисленный процент к балансу и учитывает его как
     * "накопленное вознаграждение", которое сгорит при досрочном закрытии.
     * Саму транзакцию (с id) создаёт сервисный слой — это позволяет
     * доменному объекту оставаться независимым от способа генерации id
     * и от репозитория.
     */
    public void applyMonthlyAccrual(BigDecimal interestAmount) {
        this.balance = this.balance.add(interestAmount);
        this.accruedInterestTotal = this.accruedInterestTotal.add(interestAmount);
        this.monthsAccrued++;
    }

    /**
     * Досрочное закрытие: ВСЁ вознаграждение, накопленное за счёт
     * симулированных начислений процентов, сгорает и списывается с баланса.
     * Если депозит открывался на 6 месяцев, а прогнали только 2 —
     * начисленное за эти 2 месяца сгорает целиком.
     *
     * @return сумма, которая была списана (сгоревшие проценты) — используется
     *         сервисом для создания компенсирующей транзакции INTEREST_FORFEITED.
     */
    public BigDecimal forfeitAccruedInterestAndClose() {
        if (status != DepositStatus.ACTIVE) {
            throw new IllegalStateException("Депозит уже закрыт: " + status);
        }
        if (isFullyMatured()) {
            throw new IllegalStateException(
                    "Депозит уже отработал полный срок (" + termMonths + " мес.) — "
                            + "используйте закрытие по окончании срока, проценты не сгорают");
        }
        BigDecimal forfeited = accruedInterestTotal.min(balance);
        if (forfeited.signum() > 0) {
            this.balance = this.balance.subtract(forfeited);
            this.accruedInterestTotal = BigDecimal.ZERO;
        }
        this.status = DepositStatus.CLOSED_EARLY;
        return forfeited;
    }

    @Override
    public void block() {
        super.block();
        if (status == DepositStatus.ACTIVE) {
            this.status = DepositStatus.BLOCKED;
        }
    }

    /** Закрытие по окончании срока — начисленные проценты СОХРАНЯЮТСЯ (в отличие от досрочного). */
    public void closeAtMaturity() {
        if (status != DepositStatus.ACTIVE) {
            throw new IllegalStateException("Депозит уже закрыт: " + status);
        }
        this.status = DepositStatus.CLOSED_AT_MATURITY;
    }
}
