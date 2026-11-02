package com.ptrinity.bank.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado de cuenta: la cuenta, su titular y el detalle de movimientos.
 */
public record AccountStatement(
        Account account,
        Customer customer,
        List<Transaction> transactions,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        BigDecimal totalGmf,
        LocalDateTime generatedAt
) {

    public static AccountStatement of(Account account, Customer customer, List<Transaction> transactions,
                                      LocalDateTime generatedAt) {
        BigDecimal credits = sum(transactions, MovementType.CREDIT);
        BigDecimal debits = sum(transactions, MovementType.DEBIT);
        BigDecimal gmf = transactions.stream()
                .map(Transaction::gmfAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
        return new AccountStatement(account, customer, List.copyOf(transactions), credits, debits, gmf, generatedAt);
    }

    private static BigDecimal sum(List<Transaction> transactions, MovementType movement) {
        return transactions.stream()
                .filter(t -> t.movement() == movement)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
    }
}
