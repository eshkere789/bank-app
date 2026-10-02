package com.bank.console;

import com.bank.domain.transaction.Transaction;
import com.bank.domain.transaction.TransactionType;
import com.bank.util.MoneyUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * Компактный вывод: подряд идущие транзакции одного типа сворачиваются
 * в одну строку (количество, итоговая сумма, диапазон id и описаний).
 */
public final class TransactionFormatter {

    private TransactionFormatter() {
    }

    public static void printGrouped(List<Transaction> transactions, String indent) {
        int i = 0;
        while (i < transactions.size()) {
            TransactionType type = transactions.get(i).getType();
            BigDecimal sum = BigDecimal.ZERO;
            int j = i;
            while (j < transactions.size() && transactions.get(j).getType() == type) {
                sum = sum.add(transactions.get(j).getAmount());
                j++;
            }
            int count = j - i;
            Transaction first = transactions.get(i);
            if (count == 1) {
                System.out.println(indent + first);
            } else {
                Transaction last = transactions.get(j - 1);
                System.out.printf("%s%s ×%d | итого %s%s | #%d…#%d | %s → %s%n",
                        indent, type, count, sum.signum() >= 0 ? "+" : "", MoneyUtils.normalize(sum),
                        first.getTransactionId(), last.getTransactionId(),
                        first.getDescription(), last.getDescription());
            }
            i = j;
        }
    }
}
