package com.ptrinity.bank.domain.port.out;

import com.ptrinity.bank.domain.model.AccountType;

/**
 * Puerto de salida: generación de números de cuenta únicos.
 */
public interface AccountNumberGeneratorPort {

    String nextAccountNumber(AccountType type);
}
