package com.bank.service;

import com.bank.domain.currency.Currency;
import com.bank.exception.InvalidCurrencyException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Курсы валют и конвертация. Все курсы хранятся относительно тенге:
 * сколько тенге стоит 1 единица валюты. Любая пара валют конвертируется
 * через тенге (EUR → KZT → USD), поэтому для новой валюты достаточно
 * добавить один курс.
 *
 * Стартовые значения приблизительные (по данным Нацбанка РК на начало 2026 года)
 * и меняются из консоли в разделе "Курсы валют".
 */
public class ExchangeRateService {

    private final Map<Currency, BigDecimal> kztPerUnit = new ConcurrentHashMap<>();

    public ExchangeRateService() {
        kztPerUnit.put(Currency.KZT, BigDecimal.ONE);
        kztPerUnit.put(Currency.USD, new BigDecimal("500.00"));
        kztPerUnit.put(Currency.EUR, new BigDecimal("580.00"));
    }

    /** Сколько тенге стоит 1 единица валюты. */
    public BigDecimal getRate(Currency currency) {
        return kztPerUnit.get(currency);
    }

    public void setRate(Currency currency, BigDecimal rate) {
        if (currency == Currency.KZT) {
            throw new InvalidCurrencyException("Курс тенге к самому себе менять нельзя");
        }
        if (rate == null || rate.signum() <= 0) {
            throw new InvalidCurrencyException("Курс должен быть положительным числом");
        }
        kztPerUnit.put(currency, rate);
    }

    public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        return convert(amount, from, to, RoundingMode.HALF_UP);
    }

    /** Конвертация с выбором округления (CEILING нужен, когда сумму надо покрыть "с запасом"). */
    public BigDecimal convert(BigDecimal amount, Currency from, Currency to, RoundingMode mode) {
        if (from == to) {
            return amount;
        }
        return amount.multiply(kztPerUnit.get(from)).divide(kztPerUnit.get(to), 2, mode);
    }
}
