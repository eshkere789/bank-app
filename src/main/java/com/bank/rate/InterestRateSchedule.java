package com.bank.rate;

import com.bank.domain.account.DepositType;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Таблица процентных ставок в зависимости от типа депозита и срока.
 *
 * Ставки заданы намеренно "не круглыми" числами, как в реальных банковских
 * продуктах: 17% / 17.5% / 19% / 21% / 22.3%. Накопительный депозит (без
 * права снятия) даёт более высокую ставку, чем гибкий депозит с правом
 * снятия — банк платит премию за отказ клиента от гибкости:
 *
 *              3 мес   6 мес   9 мес   12 мес
 *  withdraw:   17.0%   17.5%   19.0%   21.0%
 *  accumul.:   17.5%   19.0%   21.0%   22.3%
 */
public final class InterestRateSchedule {

    private InterestRateSchedule() {
    }

    private static final Map<Integer, BigDecimal> WITHDRAWABLE_RATES = Map.of(
            3, new BigDecimal("17.0"),
            6, new BigDecimal("17.5"),
            9, new BigDecimal("19.0"),
            12, new BigDecimal("21.0")
    );

    private static final Map<Integer, BigDecimal> ACCUMULATIVE_RATES = Map.of(
            3, new BigDecimal("17.5"),
            6, new BigDecimal("19.0"),
            9, new BigDecimal("21.0"),
            12, new BigDecimal("22.3")
    );

    public static BigDecimal rateFor(DepositType type, int termMonths) {
        Map<Integer, BigDecimal> table = type == DepositType.ACCUMULATIVE ? ACCUMULATIVE_RATES : WITHDRAWABLE_RATES;
        BigDecimal rate = table.get(termMonths);
        if (rate == null) {
            throw new IllegalArgumentException("Нет ставки для срока " + termMonths + " мес.");
        }
        return rate;
    }
}
