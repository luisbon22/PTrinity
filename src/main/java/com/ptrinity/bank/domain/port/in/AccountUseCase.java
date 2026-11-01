package com.ptrinity.bank.domain.port.in;

import com.ptrinity.bank.domain.model.Account;
import com.ptrinity.bank.domain.model.AccountStatement;
import com.ptrinity.bank.domain.model.AccountStatus;

import java.util.List;

/**
 * Puerto de entrada: casos de uso de productos financieros (cuentas).
 */
public interface AccountUseCase {

    Account open(OpenAccountCommand command);

    Account changeStatus(Long id, AccountStatus status);

    Account findById(Long id);

    List<Account> findAll(Long customerId);

    AccountStatement getStatement(Long id);
}
