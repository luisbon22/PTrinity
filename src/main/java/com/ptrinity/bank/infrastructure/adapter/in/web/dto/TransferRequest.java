package com.ptrinity.bank.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "La cuenta de origen es obligatoria")
        @Pattern(regexp = "^\\d{10}$", message = "La cuenta de origen debe tener 10 dígitos")
        String sourceAccountNumber,

        @NotBlank(message = "La cuenta de destino es obligatoria")
        @Pattern(regexp = "^\\d{10}$", message = "La cuenta de destino debe tener 10 dígitos")
        String destinationAccountNumber,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        @Digits(integer = 17, fraction = 2, message = "El monto admite máximo dos decimales")
        BigDecimal amount,

        @Size(max = 255)
        String description
) {
}
