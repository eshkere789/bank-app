package com.bank.service;

import com.bank.domain.account.Account;
import com.bank.domain.credit.Credit;
import com.bank.domain.credit.CreditStatus;
import com.bank.exception.CreditException;
import com.bank.repository.CreditRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Выдача кредитов и контроль общего лимита банка. */
public class CreditService {

    /** Банк суммарно не может иметь невозвращённых выдач больше этой суммы. */
    public static final BigDecimal BANK_CREDIT_LIMIT = new BigDecimal("1000000");

    private final CreditRepository creditRepository;
    private final AccountService accountService;
    private final CustomerService customerService;
    private final IdGeneratorService idGenerator;

    public CreditService(CreditRepository creditRepository, AccountService accountService,
                         CustomerService customerService, IdGeneratorService idGenerator) {
        this.creditRepository = creditRepository;
        this.accountService = accountService;
        this.customerService = customerService;
        this.idGenerator = idGenerator;
    }

    public Credit issueCredit(long customerId, long repaymentAccountId,
                              BigDecimal amount, int termMonths, LocalDate openDate) {
        customerService.getById(customerId);

        Account repaymentAccount = accountService.getById(repaymentAccountId);
        if (repaymentAccount.getCustomerId() != customerId) {
            throw new CreditException("Счёт списания принадлежит другому клиенту");
        }
        if (!repaymentAccount.isWithdrawalAllowed()) {
            throw new CreditException("Счёт списания должен разрешать снятие (депозит с правом снятия)");
        }
        if (repaymentAccount.isBlocked()) {
            throw new CreditException("Счёт списания заблокирован");
        }
        boolean hasDefault = creditRepository.findByCustomerId(customerId).stream()
                .anyMatch(c -> c.getStatus() == CreditStatus.DEFAULTED);
        if (hasDefault) {
            throw new CreditException("У клиента есть непогашенный просроченный кредит — новый не выдаётся");
        }

        BigDecimal available = getAvailableLimit();
        if (amount != null && amount.compareTo(available) > 0) {
            throw new CreditException("Превышен лимит банка: доступно к выдаче " + available
                    + " из " + BANK_CREDIT_LIMIT);
        }

        Credit credit = new Credit(idGenerator.nextCreditId(), customerId,
                repaymentAccountId, amount, termMonths, openDate);
        return creditRepository.save(credit);
    }

    /** Сколько банк ещё может выдать: лимит минус тела невозвращённых кредитов. */
    public BigDecimal getAvailableLimit() {
        BigDecimal used = creditRepository.findAll().stream()
                .filter(Credit::isCountedInBankLimit)
                .map(Credit::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return BANK_CREDIT_LIMIT.subtract(used);
    }

    public Credit getById(long creditId) {
        return creditRepository.findById(creditId)
                .orElseThrow(() -> new CreditException("Кредит не найден: id=" + creditId));
    }

    public List<Credit> getByCustomer(long customerId) {
        return creditRepository.findByCustomerId(customerId);
    }
}
