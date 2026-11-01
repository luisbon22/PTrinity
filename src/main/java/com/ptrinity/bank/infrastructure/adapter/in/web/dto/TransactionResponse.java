package com.ptrinity.bank.infrastructure.adapter.in.web.dto;

import com.ptrinity.bank.domain.model.MovementType;
import com.ptrinity.bank.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String reference,
        TransactionType type,
        MovementType movement,
        Long accountId,
        String accountNumber,
        BigDecimal amount,
        BigDecimal gmfAmount,
        BigDecimal balanceAfter,
        String counterpartAccountNumber,
        String description,
        LocalDateTime createdAt
) {
}
