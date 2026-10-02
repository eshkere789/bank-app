package com.bank.domain.credit;

public enum CreditStatus {
    ACTIVE,             // платежи идут
    REPAID,             // все платежи внесены штатно
    CLOSED_BY_SEIZURE,  // платёж не прошёл, но принудительное списание с депозитов покрыло весь долг
    DEFAULTED           // денег не хватило даже после списания со всех депозитов — счета заблокированы
}
