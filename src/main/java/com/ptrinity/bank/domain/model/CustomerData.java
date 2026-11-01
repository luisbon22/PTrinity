package com.ptrinity.bank.domain.model;

import java.time.LocalDate;

/**
 * Value object con los datos editables de un cliente.
 */
public record CustomerData(
        IdentificationType identificationType,
        String identificationNumber,
        String firstName,
        String lastName,
        String email,
        LocalDate birthDate
) {
}
