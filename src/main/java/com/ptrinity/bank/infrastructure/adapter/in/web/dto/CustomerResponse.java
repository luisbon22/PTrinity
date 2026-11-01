package com.ptrinity.bank.infrastructure.adapter.in.web.dto;

import com.ptrinity.bank.domain.model.IdentificationType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        IdentificationType identificationType,
        String identificationNumber,
        String firstName,
        String lastName,
        String fullName,
        String email,
        LocalDate birthDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
