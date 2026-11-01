package com.ptrinity.bank.domain.model;

/**
 * Tipos de producto permitidos. Cada tipo conoce el prefijo de su número de cuenta.
 */
public enum AccountType {
    SAVINGS("53", "Cuenta de ahorros"),
    CHECKING("33", "Cuenta corriente");

    public static final int ACCOUNT_NUMBER_LENGTH = 10;

    private final String numberPrefix;
    private final String description;

    AccountType(String numberPrefix, String description) {
        this.numberPrefix = numberPrefix;
        this.description = description;
    }

    public String getNumberPrefix() {
        return numberPrefix;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Construye un número de cuenta de 10 dígitos a partir de un consecutivo.
     */
    public String formatAccountNumber(long sequence) {
        int digits = ACCOUNT_NUMBER_LENGTH - numberPrefix.length();
        long max = (long) Math.pow(10, digits) - 1;
        if (sequence < 1 || sequence > max) {
            throw new IllegalArgumentException("Consecutivo fuera de rango para " + this + ": " + sequence);
        }
        return numberPrefix + String.format("%0" + digits + "d", sequence);
    }
}
