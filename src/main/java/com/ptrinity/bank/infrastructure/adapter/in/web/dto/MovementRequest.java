package com.ptrinity.bank.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Solicitud de consignación o retiro sobre una cuenta.
 */
public record MovementRequest(
        @NotBlank(message = "El número de cuenta es obligatorio")
        @Pattern(regexp = "^\\d{10}$", message = "El número de cuenta debe tener 10 dígitos")
        String accountNumber,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        @Digits(integer = 17, fraction = 2, message = "El monto admite máximo dos decimales")
        BigDecimal amount,

        @Size(max = 255)
        String description
) {
}
