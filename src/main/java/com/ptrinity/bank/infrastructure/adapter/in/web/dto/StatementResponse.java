package com.ptrinity.bank.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StatementResponse(
        AccountResponse account,
        CustomerResponse customer,
        List<TransactionResponse> transactions,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        BigDecimal totalGmf,
        LocalDateTime generatedAt
) {
}
