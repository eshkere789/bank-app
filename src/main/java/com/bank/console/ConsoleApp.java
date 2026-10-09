package com.bank.console;

import com.bank.domain.account.Account;
import com.bank.domain.account.DepositAccount;
import com.bank.domain.credit.Credit;
import com.bank.domain.credit.CreditStatus;
import com.bank.domain.currency.Currency;
import com.bank.domain.customer.ContactType;
import com.bank.domain.customer.Customer;
import com.bank.domain.transaction.Transaction;
import com.bank.exception.BankException;
import com.bank.repository.inmemory.InMemoryDatabase;
import com.bank.service.AccountService;
import com.bank.service.CreditRepaymentService;
import com.bank.service.CreditService;
import com.bank.service.CustomerService;
import com.bank.service.ExchangeRateService;
import com.bank.service.IdGeneratorService;
import com.bank.service.InterestAccrualService;
import com.bank.service.TransferService;
import com.bank.util.MoneyUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Консольный интерфейс приложения. Отвечает только за ввод/вывод —
 * вся бизнес-логика находится в сервисном и доменном слоях, поэтому
 * этот класс легко заменить на REST-контроллер или GUI в будущем,
 * не трогая ничего из com.bank.domain / com.bank.service.
 */
public class ConsoleApp {

    private final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    private final IdGeneratorService idGenerator = new IdGeneratorService();
    private final InMemoryDatabase database = new InMemoryDatabase();

    private final ExchangeRateService exchangeRateService = new ExchangeRateService();

    private final CustomerService customerService =
            new CustomerService(database.customers(), idGenerator);
    private final AccountService accountService =
            new AccountService(database.accounts(), database.transactions(), idGenerator);
    private final TransferService transferService =
            new TransferService(accountService, database.transactions(), idGenerator, exchangeRateService);
    private final InterestAccrualService interestAccrualService =
            new InterestAccrualService(database.transactions(), idGenerator);
    private final CreditService creditService =
            new CreditService(database.credits(), accountService, customerService, idGenerator);
    private final CreditRepaymentService creditRepaymentService =
            new CreditRepaymentService(creditService, accountService, database.transactions(), idGenerator,
                    exchangeRateService);

    public void run() {
        System.out.println("=== Консольное банковское приложение ===");
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("1. Клиенты");
            System.out.println("2. Счета и переводы");
            System.out.println("3. Депозиты");
            System.out.println("4. Кредиты");
            System.out.println("5. Симуляция времени");
            System.out.println("6. Курсы валют");
            System.out.println("0. Выход");
            System.out.print("Выберите раздел: ");
            switch (scanner.nextLine().trim()) {
                case "1" -> submenu("Клиенты",
                        new String[]{"Зарегистрировать клиента", "Список всех клиентов"},
                        this::registerCustomer, this::listCustomers);
                case "2" -> submenu("Счета и переводы",
                        new String[]{"Открыть текущий счёт (тенге)", "Открыть мультивалютный счёт (USD/EUR)",
                                "Пополнить счёт", "Снять со счёта", "Перевод между счетами (с конвертацией)",
                                "Выписка по счёту", "Все счета клиента"},
                        this::openCurrentAccount, this::openMultiCurrencyAccount,
                        this::depositMoney, this::withdrawMoney, this::transferBetweenAccounts,
                        this::showStatement, this::showCustomerAccounts);
                case "3" -> submenu("Депозиты",
                        new String[]{"Открыть депозит", "Досрочно закрыть депозит"},
                        this::openDeposit, this::closeDepositEarly);
                case "4" -> submenu("Кредиты",
                        new String[]{"Оформить кредит", "Кредиты клиента"},
                        this::openCredit, this::showCustomerCredits);
                case "5" -> submenu("Симуляция времени",
                        new String[]{"Депозит: прогнать N месяцев", "Депозит: прогнать до даты (yyyy-MM-dd)",
                                "Кредит: прогнать погашение N месяцев"},
                        this::simulateMonths, this::simulateToDate, this::simulateCreditRepayment);
                case "6" -> submenu("Курсы валют",
                        new String[]{"Показать курсы", "Изменить курс"},
                        this::showRates, this::setRate);
                case "0" -> {
                    running = false;
                    System.out.println("До свидания!");
                }
                default -> System.out.println("Неизвестный пункт меню.");
            }
        }
    }

    /**
     * Подменю раздела: остаётся открытым, пока не выбран "0. Назад",
     * поэтому несколько операций подряд не требуют возвращения в главное меню.
     */
    private void submenu(String title, String[] labels, Runnable... actions) {
        while (true) {
            System.out.println();
            System.out.println("--- " + title + " ---");
            for (int i = 0; i < labels.length; i++) {
                System.out.println((i + 1) + ". " + labels[i]);
            }
            System.out.println("0. Назад");
            System.out.print("Выберите пункт: ");
            String choice = scanner.nextLine().trim();
            if (choice.equals("0")) {
                return;
            }
            int index;
            try {
                index = Integer.parseInt(choice) - 1;
            } catch (NumberFormatException e) {
                index = -1;
            }
            if (index < 0 || index >= actions.length) {
                System.out.println("Неизвестный пункт меню.");
                continue;
            }
            try {
                actions[index].run();
            } catch (BankException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Некорректный ввод: " + e.getMessage());
            }
        }
    }

    private void registerCustomer() {
        System.out.print("Имя: ");
        String firstName = scanner.nextLine().trim();
        System.out.print("Фамилия: ");
        String lastName = scanner.nextLine().trim();
        System.out.print("Уникальный признак (1-email, 2-телефон, 3-пароль): ");
        String typeChoice = scanner.nextLine().trim();
        ContactType type = switch (typeChoice) {
            case "1" -> ContactType.EMAIL;
            case "2" -> ContactType.PHONE;
            case "3" -> ContactType.PASSWORD;
            default -> throw new IllegalArgumentException("Неверный выбор типа признака");
        };
        System.out.print("Значение (" + type + "): ");
        String value = scanner.nextLine().trim();

        Customer customer = customerService.registerCustomer(firstName, lastName, type, value);
        System.out.println("Клиент создан: " + customer);
    }

    private void openDeposit() {
        System.out.print("ID клиента: ");
        long customerId = Long.parseLong(scanner.nextLine().trim());
        customerService.getById(customerId); // проверка существования

        System.out.print("Тип депозита (1-с пополнением и снятием, 2-только пополнение): ");
        String typeChoice = scanner.nextLine().trim();

        System.out.print("Срок депозита в месяцах (3, 6, 9 или 12): ");
        int termMonths = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Начальный взнос (Enter — 0): ");
        String balanceInput = scanner.nextLine().trim();
        BigDecimal initialBalance = balanceInput.isBlank() ? null : new BigDecimal(balanceInput);

        Currency currency = readCurrency(true);
        LocalDate openDate = LocalDate.now();

        Account account = switch (typeChoice) {
            case "1" -> accountService.openWithdrawableDeposit(customerId, initialBalance, termMonths, openDate, currency);
            case "2" -> accountService.openAccumulativeDeposit(customerId, initialBalance, termMonths, openDate, currency);
            default -> throw new IllegalArgumentException("Неверный выбор типа депозита");
        };

        DepositAccount deposit = (DepositAccount) account;
        System.out.printf("Депозит открыт: %s (%s), ставка=%s%%, дата открытия=%s, дата окончания=%s%n",
                deposit.getAccountNumber(), deposit.getCurrency(), deposit.getInterestRate(), deposit.getOpenDate(), deposit.getEndDate());
    }

    private void depositMoney() {
        long accountId = readAccountId();
        BigDecimal amount = readAmount("Сумма пополнения: ");
        Transaction t = accountService.deposit(accountId, amount);
        System.out.println("Готово: " + t);
    }

    private void withdrawMoney() {
        long accountId = readAccountId();
        BigDecimal amount = readAmount("Сумма снятия: ");
        Transaction t = accountService.withdraw(accountId, amount);
        System.out.println("Готово: " + t);
    }

    private void transferBetweenAccounts() {
        System.out.print("ID счёта-источника: ");
        long fromId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("ID счёта-получателя: ");
        long toId = Long.parseLong(scanner.nextLine().trim());
        Account from = accountService.getById(fromId);
        Account to = accountService.getById(toId);

        BigDecimal amount = readAmount("Сумма перевода (в " + from.getCurrency() + "): ");
        BigDecimal credited = transferService.transfer(fromId, toId, amount);

        if (from.getCurrency() == to.getCurrency()) {
            System.out.printf("Перевод выполнен: %s %s%n", MoneyUtils.normalize(amount), from.getCurrency());
        } else {
            StringBuilder rates = new StringBuilder();
            for (Currency c : new Currency[]{from.getCurrency(), to.getCurrency()}) {
                if (c != Currency.KZT) {
                    rates.append(rates.length() > 0 ? ", " : "")
                            .append("1 ").append(c).append(" = ")
                            .append(MoneyUtils.normalize(exchangeRateService.getRate(c))).append(" KZT");
                }
            }
            System.out.printf("Перевод выполнен: списано %s %s → зачислено %s %s (курс: %s)%n",
                    MoneyUtils.normalize(amount), from.getCurrency(),
                    MoneyUtils.normalize(credited), to.getCurrency(), rates);
        }
    }

    private void openCurrentAccount() {
        System.out.print("ID клиента: ");
        long customerId = Long.parseLong(scanner.nextLine().trim());
        customerService.getById(customerId);
        BigDecimal initialBalance = readInitialBalance();
        Account account = accountService.openCurrentAccount(customerId, initialBalance);
        System.out.println("Текущий счёт открыт: " + describe(account));
    }

    private void openMultiCurrencyAccount() {
        System.out.print("ID клиента: ");
        long customerId = Long.parseLong(scanner.nextLine().trim());
        customerService.getById(customerId);
        Currency currency = readCurrency(false);
        BigDecimal initialBalance = readInitialBalance();
        Account account = accountService.openMultiCurrencyAccount(customerId, currency, initialBalance);
        System.out.println("Мультивалютный счёт открыт: " + describe(account));
    }

    private void showRates() {
        for (Currency currency : Currency.values()) {
            if (currency != Currency.KZT) {
                System.out.println("1 " + currency + " = "
                        + MoneyUtils.normalize(exchangeRateService.getRate(currency)) + " KZT");
            }
        }
    }

    private void setRate() {
        Currency currency = readCurrency(false);
        BigDecimal rate = readAmount("Новый курс (тенге за 1 " + currency + "): ");
        exchangeRateService.setRate(currency, rate);
        System.out.println("Курс обновлён: 1 " + currency + " = " + MoneyUtils.normalize(rate) + " KZT");
    }

    private void simulateMonths() {
        long accountId = readAccountId();
        System.out.print("Сколько месяцев прогнать: ");
        int months = Integer.parseInt(scanner.nextLine().trim());
        DepositAccount deposit = accountService.getDepositById(accountId);
        List<Transaction> accruals = interestAccrualService.simulateMonths(deposit, months);
        TransactionFormatter.printGrouped(accruals, "  ");
        System.out.println("Текущий баланс: " + MoneyUtils.normalize(deposit.getBalance()) + " " + deposit.getCurrency());
    }

    private void simulateToDate() {
        long accountId = readAccountId();
        System.out.print("Дата (yyyy-MM-dd): ");
        LocalDate date = LocalDate.parse(scanner.nextLine().trim());
        DepositAccount deposit = accountService.getDepositById(accountId);
        List<Transaction> accruals = interestAccrualService.simulateToDate(deposit, date);
        TransactionFormatter.printGrouped(accruals, "  ");
        System.out.println("Текущий баланс: " + MoneyUtils.normalize(deposit.getBalance()) + " " + deposit.getCurrency());
    }

    private void closeDepositEarly() {
        long accountId = readAccountId();
        DepositAccount deposit = accountService.closeDepositEarly(accountId);
        System.out.println("Депозит " + deposit.getAccountNumber() + " закрыт досрочно. Баланс: "
                + MoneyUtils.normalize(deposit.getBalance()) + " " + deposit.getCurrency());
    }

    private void showStatement() {
        long accountId = readAccountId();
        Account account = accountService.getById(accountId);
        System.out.println("Счёт: " + account.getAccountNumber() + " (" + account.getAccountType() + "), баланс: "
                + MoneyUtils.normalize(account.getBalance()) + " " + account.getCurrency());
        TransactionFormatter.printGrouped(account.getTransactions(), "  ");
    }

    private void showCustomerAccounts() {
        System.out.print("ID клиента: ");
        long customerId = Long.parseLong(scanner.nextLine().trim());
        List<Account> accounts = accountService.getAccountsByCustomer(customerId);
        if (accounts.isEmpty()) {
            System.out.println("Счетов не найдено.");
            return;
        }
        for (Account account : accounts) {
            System.out.println(describe(account));
        }
    }

    private void listCustomers() {
        List<Customer> customers = customerService.getAll();
        if (customers.isEmpty()) {
            System.out.println("Клиентов нет.");
            return;
        }
        customers.forEach(System.out::println);
    }

    private void openCredit() {
        System.out.print("ID клиента: ");
        long customerId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("ID счёта списания (в тенге, с правом снятия): ");
        long accountId = Long.parseLong(scanner.nextLine().trim());
        BigDecimal amount = readAmount("Сумма кредита: ");
        System.out.print("Срок кредита в месяцах (6, 12, 24 или 36): ");
        int termMonths = Integer.parseInt(scanner.nextLine().trim());

        Credit credit = creditService.issueCredit(customerId, accountId, amount, termMonths, LocalDate.now());
        System.out.println("Кредит одобрен: " + describeCredit(credit));
        System.out.println("Свободный лимит банка: " + MoneyUtils.normalize(creditService.getAvailableLimit()));
    }

    private void simulateCreditRepayment() {
        System.out.print("ID кредита: ");
        long creditId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("Сколько месяцев прогнать: ");
        int months = Integer.parseInt(scanner.nextLine().trim());

        List<Transaction> operations = creditRepaymentService.simulateMonths(creditId, months);
        TransactionFormatter.printGrouped(operations, "  ");

        Credit credit = creditService.getById(creditId);
        if (credit.getStatus() == CreditStatus.DEFAULTED) {
            System.out.println("!!! Средств не хватило: все счета клиента заблокированы, депозиты списаны в счёт кредита.");
        } else if (credit.getStatus() == CreditStatus.CLOSED_BY_SEIZURE) {
            System.out.println("Платёж не прошёл, но долг полностью покрыт принудительным списанием с депозитов.");
        }
        System.out.println(describeCredit(credit));
    }

    private void showCustomerCredits() {
        System.out.print("ID клиента: ");
        long customerId = Long.parseLong(scanner.nextLine().trim());
        List<Credit> credits = creditService.getByCustomer(customerId);
        if (credits.isEmpty()) {
            System.out.println("Кредитов не найдено.");
            return;
        }
        credits.forEach(c -> System.out.println(describeCredit(c)));
    }

    private String describeCredit(Credit c) {
        String debt = c.getStatus() == CreditStatus.DEFAULTED
                ? " | НЕПОГАШЕННЫЙ ДОЛГ=" + MoneyUtils.normalize(c.getUnpaidDebt())
                : " | остаток основного долга=" + MoneyUtils.normalize(c.getRemainingPrincipal());
        return String.format("Кредит #%d | клиент=%d | статус=%s | сумма=%s | ставка=%s%% | срок=%d мес | "
                        + "платёж=%s | оплачено платежей=%d/%d | выплачено всего=%s | до=%s%s",
                c.getCreditId(), c.getCustomerId(), c.getStatus(), MoneyUtils.normalize(c.getPrincipal()),
                Credit.ANNUAL_RATE, c.getTermMonths(), MoneyUtils.normalize(c.getMonthlyPayment()),
                c.getPaidInstallments(), c.getTermMonths(), MoneyUtils.normalize(c.getTotalPaid()),
                c.getEndDate(), debt);
    }

    private String describe(Account account) {
        if (account instanceof DepositAccount d) {
            return String.format(
                    "%s | тип=%s | валюта=%s | ставка=%s%% | срок=%d мес | открыт=%s | закрывается=%s | статус=%s | баланс=%s",
                    d.getAccountNumber(), d.getAccountType(), d.getCurrency(), d.getInterestRate(), d.getTermMonths(),
                    d.getOpenDate(), d.getEndDate(), d.getStatus(), MoneyUtils.normalize(d.getBalance()));
        }
        return String.format("%s | тип=%s | валюта=%s | баланс=%s%s",
                account.getAccountNumber(), account.getAccountType(), account.getCurrency(),
                MoneyUtils.normalize(account.getBalance()), account.isBlocked() ? " | ЗАБЛОКИРОВАН" : "");
    }

    /** @param allowKzt true — депозит (KZT/USD/EUR), false — мультивалютный счёт и курсы (USD/EUR) */
    private Currency readCurrency(boolean allowKzt) {
        System.out.print(allowKzt ? "Валюта (1-KZT, 2-USD, 3-EUR): " : "Валюта (1-USD, 2-EUR): ");
        String choice = scanner.nextLine().trim();
        if (allowKzt) {
            return switch (choice) {
                case "1" -> Currency.KZT;
                case "2" -> Currency.USD;
                case "3" -> Currency.EUR;
                default -> throw new IllegalArgumentException("Неверный выбор валюты");
            };
        }
        return switch (choice) {
            case "1" -> Currency.USD;
            case "2" -> Currency.EUR;
            default -> throw new IllegalArgumentException("Неверный выбор валюты");
        };
    }

    private BigDecimal readInitialBalance() {
        System.out.print("Начальная сумма (Enter — 0): ");
        String input = scanner.nextLine().trim();
        return input.isBlank() ? null : new BigDecimal(input);
    }

    private long readAccountId() {
        System.out.print("ID счёта: ");
        return Long.parseLong(scanner.nextLine().trim());
    }

    private BigDecimal readAmount(String prompt) {
        System.out.print(prompt);
        return new BigDecimal(scanner.nextLine().trim());
    }
}
