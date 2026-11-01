package com.ptrinity.bank.infrastructure.adapter.in.web.dto;

import com.ptrinity.bank.domain.model.AccountType;
import jakarta.validation.constraints.NotNull;

public record AccountRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long customerId,

        @NotNull(message = "El tipo de cuenta es obligatorio")
        AccountType accountType,

        boolean gmfExempt
) {
}
