package com.ptrinity.bank.domain.port.in;

import java.math.BigDecimal;

public record WithdrawalCommand(String accountNumber, BigDecimal amount, String description) {
}
