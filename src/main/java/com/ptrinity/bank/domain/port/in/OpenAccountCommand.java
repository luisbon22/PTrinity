package com.ptrinity.bank.domain.port.in;

import com.ptrinity.bank.domain.model.AccountType;

public record OpenAccountCommand(Long customerId, AccountType accountType, boolean gmfExempt) {
}
