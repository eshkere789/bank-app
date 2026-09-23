package com.bank.domain.customer;

import java.util.Objects;

/**
 * Клиент банка. Неизменяемый объект: персональные данные не меняются
 * после создания — только пересозданием через Builder.
 *
 * Собран через Builder намеренно: набор полей (в т.ч. взаимоисключающий
 * выбор email/телефон/пароль) удобно валидировать в момент build(),
 * не давая создать клиента в неполном/невалидном состоянии.
 */
public final class Customer {

    private final long id;
    private final String firstName;
    private final String lastName;
    private final ContactType contactType;
    private final String uniqueContact;

    private Customer(Builder builder) {
        this.id = builder.id;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.contactType = builder.contactType;
        this.uniqueContact = builder.uniqueContact;
    }

    public long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public ContactType getContactType() {
        return contactType;
    }

    public String getUniqueContact() {
        return uniqueContact;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer customer = (Customer) o;
        return id == customer.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Customer{id=" + id + ", name='" + getFullName() + "', "
                + contactType + "='" + maskContact() + "'}";
    }

    /** Маскируем чувствительные данные (особенно пароль) при выводе в консоль. */
    private String maskContact() {
        if (contactType == ContactType.PASSWORD) {
            return "*".repeat(Math.max(uniqueContact.length(), 4));
        }
        return uniqueContact;
    }

    public static final class Builder {
        private long id;
        private String firstName;
        private String lastName;
        private ContactType contactType;
        private String uniqueContact;

        private Builder() {
        }

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = Objects.requireNonNull(firstName, "firstName");
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = Objects.requireNonNull(lastName, "lastName");
            return this;
        }

        /** Единственный уникальный признак клиента: email, телефон ИЛИ пароль. */
        public Builder uniqueContact(ContactType type, String value) {
            this.contactType = Objects.requireNonNull(type, "contactType");
            this.uniqueContact = Objects.requireNonNull(value, "uniqueContact");
            return this;
        }

        public Customer build() {
            if (firstName == null || firstName.isBlank()) {
                throw new IllegalStateException("Имя клиента обязательно");
            }
            if (lastName == null || lastName.isBlank()) {
                throw new IllegalStateException("Фамилия клиента обязательна");
            }
            if (contactType == null || uniqueContact == null || uniqueContact.isBlank()) {
                throw new IllegalStateException(
                        "Нужно указать ровно один уникальный признак: email, телефон или пароль");
            }
            return new Customer(this);
        }
    }
}
