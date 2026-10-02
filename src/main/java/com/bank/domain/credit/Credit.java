package com.bank.domain.credit;

import com.bank.exception.CreditException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Set;

/**
 * Кредит с аннуитетными платежами (одинаковый ежемесячный платёж,
 * в который уже включены проценты). Ставка фиксирована — 30% годовых.
 */
public class Credit {

    public static final BigDecimal ANNUAL_RATE = new BigDecimal("30.0");
    public static final Set<Integer> ALLOWED_TERMS_MONTHS = Set.of(6, 12, 24, 36);

    private static final BigDecimal MONTHLY_RATE =
            ANNUAL_RATE.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

    private final long creditId;
    private final long customerId;
    private final long repaymentAccountId;
    private final BigDecimal principal;
    private final int termMonths;
    private final LocalDate openDate;
    private final LocalDate endDate;
    private final BigDecimal monthlyPayment;

    private BigDecimal remainingPrincipal;
    private int paidInstallments = 0;
    private BigDecimal totalPaid = BigDecimal.ZERO;
    private BigDecimal unpaidDebt = BigDecimal.ZERO; // остаток долга после дефолта
    private CreditStatus status = CreditStatus.ACTIVE;

    public Credit(long creditId, long customerId, long repaymentAccountId,
                  BigDecimal principal, int termMonths, LocalDate openDate) {
        if (principal == null || principal.signum() <= 0) {
            throw new CreditException("Сумма кредита должна быть положительной");
        }
        if (!ALLOWED_TERMS_MONTHS.contains(termMonths)) {
            throw new CreditException("Недопустимый срок кредита: " + termMonths
                    + " мес. Разрешено: " + ALLOWED_TERMS_MONTHS);
        }
        this.creditId = creditId;
        this.customerId = customerId;
        this.repaymentAccountId = repaymentAccountId;
        this.principal = principal.setScale(2, RoundingMode.HALF_UP);
        this.termMonths = termMonths;
        this.openDate = openDate;
        this.endDate = openDate.plusMonths(termMonths);
        this.monthlyPayment = calculateAnnuityPayment(this.principal, termMonths);
        this.remainingPrincipal = this.principal;
    }

    /** Аннуитет: P * r / (1 - (1 + r)^-n). */
    private static BigDecimal calculateAnnuityPayment(BigDecimal principal, int months) {
        BigDecimal factor = BigDecimal.ONE.add(MONTHLY_RATE).pow(months);
        BigDecimal denominator = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(factor, 20, RoundingMode.HALF_UP));
        return principal.multiply(MONTHLY_RATE).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /** Проценты за текущий месяц — от остатка основного долга. */
    public BigDecimal currentMonthInterest() {
        return remainingPrincipal.multiply(MONTHLY_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /** Сумма очередного платежа; последний платёж гасит остаток без копеечной погрешности. */
    public BigDecimal nextInstallmentAmount() {
        if (paidInstallments == termMonths - 1) {
            return remainingPrincipal.add(currentMonthInterest());
        }
        return monthlyPayment;
    }

    /** Долг, который банк взыскивает при дефолте: остаток основного долга + проценты за текущий месяц. */
    public BigDecimal debtForCollection() {
        return remainingPrincipal.add(currentMonthInterest());
    }

    /** Учесть штатно внесённый платёж (деньги со счёта списывает сервис). */
    public void applyPaidInstallment() {
        BigDecimal payment = nextInstallmentAmount();
        BigDecimal principalPart = payment.subtract(currentMonthInterest());
        this.remainingPrincipal = this.remainingPrincipal.subtract(principalPart);
        this.totalPaid = this.totalPaid.add(payment);
        this.paidInstallments++;
        if (paidInstallments >= termMonths) {
            this.status = CreditStatus.REPAID;
        }
    }

    /** Учесть принудительное списание со счетов клиента (дефолт по платежу). */
    public void applySeizure(BigDecimal collected, BigDecimal debtBefore) {
        this.totalPaid = this.totalPaid.add(collected);
        if (collected.compareTo(debtBefore) >= 0) {
            this.remainingPrincipal = BigDecimal.ZERO;
            this.unpaidDebt = BigDecimal.ZERO;
            this.status = CreditStatus.CLOSED_BY_SEIZURE;
        } else {
            this.unpaidDebt = debtBefore.subtract(collected);
            this.status = CreditStatus.DEFAULTED;
        }
    }

    /** Считается ли кредит в общем лимите выдач банка. */
    public boolean isCountedInBankLimit() {
        return status == CreditStatus.ACTIVE || status == CreditStatus.DEFAULTED;
    }

    public long getCreditId() { return creditId; }
    public long getCustomerId() { return customerId; }
    public long getRepaymentAccountId() { return repaymentAccountId; }
    public BigDecimal getPrincipal() { return principal; }
    public int getTermMonths() { return termMonths; }
    public LocalDate getOpenDate() { return openDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public BigDecimal getRemainingPrincipal() { return remainingPrincipal; }
    public int getPaidInstallments() { return paidInstallments; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public BigDecimal getUnpaidDebt() { return unpaidDebt; }
    public CreditStatus getStatus() { return status; }
}
