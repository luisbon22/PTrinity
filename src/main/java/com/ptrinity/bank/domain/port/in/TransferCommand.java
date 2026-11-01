package com.ptrinity.bank.domain.port.in;

import java.math.BigDecimal;

public record TransferCommand(String sourceAccountNumber, String destinationAccountNumber,
                              BigDecimal amount, String description) {
}
