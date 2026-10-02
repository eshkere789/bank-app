package com.bank.console;

import com.bank.domain.account.Account;
import com.bank.domain.account.DepositAccount;
import com.bank.domain.customer.ContactType;
import com.bank.domain.customer.Customer;
import com.bank.domain.transaction.Transaction;
import com.bank.exception.BankException;
import com.bank.repository.inmemory.InMemoryDatabase;
import com.bank.service.AccountService;
import com.bank.service.CustomerService;
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

    private final CustomerService customerService =
            new CustomerService(database.customers(), idGenerator);
    private final AccountService accountService =
            new AccountService(database.accounts(), database.transactions(), idGenerator);
    private final TransferService transferService =
            new TransferService(accountService, database.transactions(), idGenerator);
    private final InterestAccrualService interestAccrualService =
            new InterestAccrualService(database.transactions(), idGenerator);

    public void run() {
        System.out.println("=== Консольное банковское приложение ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> registerCustomer();
                    case "2" -> openDeposit();
                    case "3" -> depositMoney();
                    case "4" -> withdrawMoney();
                    case "5" -> transferBetweenDeposits();
                    case "6" -> simulateMonths();
                    case "7" -> simulateToDate();
                    case "8" -> closeDepositEarly();
                    case "9" -> showStatement();
                    case "10" -> showCustomerAccounts();
                    case "11" -> listCustomers();
                    case "0" -> {
                        running = false;
                        System.out.println("До свидания!");
                    }
                    default -> System.out.println("Неизвестный пункт меню.");
                }
            } catch (BankException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Некорректный ввод: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1.  Зарегистрировать клиента");
        System.out.println("2.  Открыть депозит");
        System.out.println("3.  Пополнить депозит");
        System.out.println("4.  Снять с депозита");
        System.out.println("5.  Перевод между депозитами");
        System.out.println("6.  Симулировать N месяцев");
        System.out.println("7.  Симулировать до даты (yyyy-MM-dd)");
        System.out.println("8.  Досрочно закрыть депозит");
        System.out.println("9.  Выписка по депозиту");
        System.out.println("10. Депозиты клиента");
        System.out.println("11. Список всех клиентов");
        System.out.println("0.  Выход");
        System.out.print("Выберите пункт: ");
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

        LocalDate openDate = LocalDate.now();

        Account account = switch (typeChoice) {
            case "1" -> accountService.openWithdrawableDeposit(customerId, initialBalance, termMonths, openDate);
            case "2" -> accountService.openAccumulativeDeposit(customerId, initialBalance, termMonths, openDate);
            default -> throw new IllegalArgumentException("Неверный выбор типа депозита");
        };

        DepositAccount deposit = (DepositAccount) account;
        System.out.printf("Депозит открыт: %s, ставка=%s%%, дата открытия=%s, дата окончания=%s%n",
                deposit.getAccountNumber(), deposit.getInterestRate(), deposit.getOpenDate(), deposit.getEndDate());
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

    private void transferBetweenDeposits() {
        System.out.print("ID счёта-источника: ");
        long fromId = Long.parseLong(scanner.nextLine().trim());
        System.out.print("ID счёта-получателя: ");
        long toId = Long.parseLong(scanner.nextLine().trim());
        BigDecimal amount = readAmount("Сумма перевода: ");
        transferService.transfer(fromId, toId, amount);
        System.out.println("Перевод выполнен.");
    }

    private void simulateMonths() {
        long accountId = readAccountId();
        System.out.print("Сколько месяцев прогнать: ");
        int months = Integer.parseInt(scanner.nextLine().trim());
        DepositAccount deposit = accountService.getDepositById(accountId);
        List<Transaction> accruals = interestAccrualService.simulateMonths(deposit, months);
        accruals.forEach(System.out::println);
        System.out.println("Текущий баланс: " + MoneyUtils.normalize(deposit.getBalance()));
    }

    private void simulateToDate() {
        long accountId = readAccountId();
        System.out.print("Дата (yyyy-MM-dd): ");
        LocalDate date = LocalDate.parse(scanner.nextLine().trim());
        DepositAccount deposit = accountService.getDepositById(accountId);
        List<Transaction> accruals = interestAccrualService.simulateToDate(deposit, date);
        accruals.forEach(System.out::println);
        System.out.println("Текущий баланс: " + MoneyUtils.normalize(deposit.getBalance()));
    }

    private void closeDepositEarly() {
        long accountId = readAccountId();
        DepositAccount deposit = accountService.closeDepositEarly(accountId);
        System.out.println("Депозит " + deposit.getAccountNumber() + " закрыт досрочно. Баланс: "
                + MoneyUtils.normalize(deposit.getBalance()));
    }

    private void showStatement() {
        long accountId = readAccountId();
        Account account = accountService.getById(accountId);
        System.out.println("Счёт: " + account.getAccountNumber() + ", баланс: "
                + MoneyUtils.normalize(account.getBalance()));
        for (Transaction t : account.getTransactions()) {
            System.out.println("  " + t);
        }
    }

    private void showCustomerAccounts() {
        System.out.print("ID клиента: ");
        long customerId = Long.parseLong(scanner.nextLine().trim());
        List<Account> accounts = accountService.getAccountsByCustomer(customerId);
        if (accounts.isEmpty()) {
            System.out.println("Депозитов не найдено.");
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

    private String describe(Account account) {
        if (account instanceof DepositAccount d) {
            return String.format(
                    "%s | тип=%s | ставка=%s%% | срок=%d мес | открыт=%s | закрывается=%s | статус=%s | баланс=%s",
                    d.getAccountNumber(), d.getDepositType(), d.getInterestRate(), d.getTermMonths(),
                    d.getOpenDate(), d.getEndDate(), d.getStatus(), MoneyUtils.normalize(d.getBalance()));
        }
        return account.getAccountNumber() + " | баланс=" + MoneyUtils.normalize(account.getBalance());
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
