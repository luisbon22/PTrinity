package com.ptrinity.bank.domain.port.out;

import com.ptrinity.bank.domain.model.Transaction;

import java.util.List;

/**
 * Puerto de salida: persistencia de movimientos.
 */
public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findAll();
}
