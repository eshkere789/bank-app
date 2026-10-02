package com.bank.domain.customer;

/**
 * Тип уникального идентификатора клиента.
 * По условию у клиента ровно ОДИН уникальный признак —
 * email, телефон или пароль (выбирается при регистрации).
 */
public enum ContactType {
    EMAIL,
    PHONE,
    PASSWORD
}
