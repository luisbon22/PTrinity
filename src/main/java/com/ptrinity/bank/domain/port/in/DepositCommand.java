package com.ptrinity.bank.domain.port.in;

import java.math.BigDecimal;

public record DepositCommand(String accountNumber, BigDecimal amount, String description) {
}
