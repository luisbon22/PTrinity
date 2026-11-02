package com.ptrinity.bank.domain.model;

public enum IdentificationType {
    CC("Cédula de ciudadanía"),
    CE("Cédula de extranjería"),
    PA("Pasaporte"),
    PPT("Permiso por protección temporal");

    private final String description;

    IdentificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
