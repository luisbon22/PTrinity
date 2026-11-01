package com.ptrinity.bank.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento financiero registrado sobre una cuenta. Es inmutable: un movimiento
 * contable nunca se edita ni se elimina, solo se consulta.
 *
 * @param reference agrupa los movimientos de una misma operación
 *                  (una transferencia genera un débito y un crédito con la misma referencia)
 */
public record Transaction(
        Long id,
        String reference,
        TransactionType type,
        MovementType movement,
        Long accountId,
        String accountNumber,
        BigDecimal amount,
        BigDecimal gmfAmount,
        BigDecimal balanceAfter,
        String counterpartAccountNumber,
        String description,
        LocalDateTime createdAt
) {

    public static Transaction credit(String reference, TransactionType type, Account account, BigDecimal amount,
                                     String counterpartAccountNumber, String description, LocalDateTime now) {
        return new Transaction(null, reference, type, MovementType.CREDIT, account.getId(),
                account.getAccountNumber(), amount, BigDecimal.ZERO.setScale(2), account.getBalance(),
                counterpartAccountNumber, description, now);
    }

    public static Transaction debit(String reference, TransactionType type, Account account, BigDecimal amount,
                                    BigDecimal gmf, String counterpartAccountNumber, String description,
                                    LocalDateTime now) {
        return new Transaction(null, reference, type, MovementType.DEBIT, account.getId(),
                account.getAccountNumber(), amount, gmf, account.getBalance(),
                counterpartAccountNumber, description, now);
    }
}
