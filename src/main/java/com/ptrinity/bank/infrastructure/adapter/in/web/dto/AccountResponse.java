package com.ptrinity.bank.infrastructure.adapter.in.web.dto;

import com.ptrinity.bank.domain.model.AccountStatus;
import com.ptrinity.bank.domain.model.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        AccountType accountType,
        String accountNumber,
        AccountStatus status,
        BigDecimal balance,
        BigDecimal availableBalance,
        BigDecimal overdraftLimit,
        boolean gmfExempt,
        Long customerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
