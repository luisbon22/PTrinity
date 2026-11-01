package com.ptrinity.bank.domain.port.out;

import com.ptrinity.bank.domain.model.Account;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia de cuentas.
 */
public interface AccountRepositoryPort {

    Account save(Account account);

    Optional<Account> findById(Long id);

    /**
     * Obtiene la cuenta bloqueándola para escritura (bloqueo pesimista) hasta
     * que finalice la transacción, garantizando aislamiento en operaciones concurrentes.
     */
    Optional<Account> findByAccountNumberForUpdate(String accountNumber);

    List<Account> findAll();

    List<Account> findByCustomerId(Long customerId);

    boolean existsByCustomerId(Long customerId);
}
