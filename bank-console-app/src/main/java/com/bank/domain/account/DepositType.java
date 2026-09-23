package com.bank.domain.account;

/**
 * Тип депозита.
 * WITHDRAWABLE  — пополнение И снятие разрешены.
 * ACCUMULATIVE  — только пополнение, снятие запрещено до окончания срока.
 */
public enum DepositType {
    WITHDRAWABLE,
    ACCUMULATIVE
}
