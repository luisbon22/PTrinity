package com.ptrinity.bank.infrastructure.adapter.in.web.dto;

import com.ptrinity.bank.domain.model.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(
        @NotNull(message = "El estado es obligatorio")
        AccountStatus status
) {
}
