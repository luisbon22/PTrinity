package com.ptrinity.bank.domain.port.in;

import com.ptrinity.bank.domain.model.Transaction;

import java.util.List;

/**
 * Puerto de entrada: movimientos financieros (consignación, retiro y transferencia).
 */
public interface TransactionUseCase {

    Transaction deposit(DepositCommand command);

    Transaction withdraw(WithdrawalCommand command);

    /**
     * @return los dos movimientos generados: débito en origen y crédito en destino.
     */
    List<Transaction> transfer(TransferCommand command);

    List<Transaction> findAll(Long accountId);
}
