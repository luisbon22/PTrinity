package com.ptrinity.bank.domain.model;

public enum TransactionType {
    DEPOSIT("Consignación"),
    WITHDRAWAL("Retiro"),
    TRANSFER("Transferencia");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
