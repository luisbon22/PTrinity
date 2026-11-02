package com.ptrinity.bank.domain.model;

import com.ptrinity.bank.domain.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Agregado Cuenta (producto financiero). Encapsula el saldo y el ciclo de vida
 * de la cuenta: solo se modifica a través de operaciones que validan las reglas.
 */
public class Account {

    /** Gravamen a los movimientos financieros: 4 x 1.000. */
    public static final BigDecimal GMF_RATE = new BigDecimal("0.004");

    private final Long id;
    private final AccountType accountType;
    private final String accountNumber;
    private AccountStatus status;
    private BigDecimal balance;
    private final BigDecimal overdraftLimit;
    private final boolean gmfExempt;
    private final Long customerId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Account(Long id, AccountType accountType, String accountNumber, AccountStatus status,
                   BigDecimal balance, BigDecimal overdraftLimit, boolean gmfExempt, Long customerId,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.accountType = accountType;
        this.accountNumber = accountNumber;
        this.status = status;
        this.balance = balance;
        this.overdraftLimit = overdraftLimit == null ? BigDecimal.ZERO : overdraftLimit;
        this.gmfExempt = gmfExempt;
        this.customerId = customerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method para abrir una cuenta nueva. La cuenta nace activa, con saldo cero
     * y fecha de creación calculada automáticamente. Las cuentas de ahorro nunca
     * tienen sobregiro, por lo que su saldo no puede ser menor a cero.
     */
    public static Account open(AccountType type, String accountNumber, Long customerId,
                               boolean gmfExempt, BigDecimal checkingOverdraftLimit, LocalDateTime now) {
        if (type == null) {
            throw new BusinessRuleException("El tipo de cuenta es obligatorio");
        }
        if (customerId == null) {
            throw new BusinessRuleException("La cuenta debe estar vinculada a un cliente");
        }
        if (accountNumber == null || !accountNumber.matches("\\d{" + AccountType.ACCOUNT_NUMBER_LENGTH + "}")
                || !accountNumber.startsWith(type.getNumberPrefix())) {
            throw new BusinessRuleException("Número de cuenta inválido para " + type.getDescription());
        }
        BigDecimal overdraft = type == AccountType.CHECKING && checkingOverdraftLimit != null
                ? checkingOverdraftLimit
                : BigDecimal.ZERO;
        return new Account(null, type, accountNumber, AccountStatus.ACTIVE, money(BigDecimal.ZERO),
                money(overdraft), gmfExempt, customerId, now, null);
    }

    /** Saldo disponible = saldo + cupo de sobregiro (solo cuentas corrientes). */
    public BigDecimal getAvailableBalance() {
        return balance.add(overdraftLimit);
    }

    public BigDecimal calculateGmf(BigDecimal amount) {
        if (gmfExempt) {
            return money(BigDecimal.ZERO);
        }
        return money(amount.multiply(GMF_RATE));
    }

    /**
     * Debita el monto más el GMF correspondiente. Retorna el GMF cobrado.
     */
    public BigDecimal debit(BigDecimal amount, LocalDateTime now) {
        validateAmount(amount);
        ensureActive();
        BigDecimal gmf = calculateGmf(amount);
        BigDecimal total = amount.add(gmf);
        if (getAvailableBalance().compareTo(total) < 0) {
            throw new BusinessRuleException("Saldo insuficiente en la cuenta " + accountNumber);
        }
        BigDecimal newBalance = balance.subtract(total);
        if (accountType == AccountType.SAVINGS && newBalance.signum() < 0) {
            throw new BusinessRuleException("La cuenta de ahorros no puede quedar con saldo negativo");
        }
        this.balance = money(newBalance);
        this.updatedAt = now;
        return gmf;
    }

    public void credit(BigDecimal amount, LocalDateTime now) {
        validateAmount(amount);
        ensureActive();
        this.balance = money(balance.add(amount));
        this.updatedAt = now;
    }

    public void activate(LocalDateTime now) {
        if (status == AccountStatus.CANCELLED) {
            throw new BusinessRuleException("Una cuenta cancelada no puede activarse");
        }
        this.status = AccountStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        if (status == AccountStatus.CANCELLED) {
            throw new BusinessRuleException("Una cuenta cancelada no puede inactivarse");
        }
        this.status = AccountStatus.INACTIVE;
        this.updatedAt = now;
    }

    public void cancel(LocalDateTime now) {
        if (status == AccountStatus.CANCELLED) {
            throw new BusinessRuleException("La cuenta ya se encuentra cancelada");
        }
        if (balance.signum() != 0) {
            throw new BusinessRuleException("Solo se pueden cancelar cuentas con saldo igual a $0");
        }
        this.status = AccountStatus.CANCELLED;
        this.updatedAt = now;
    }

    public void changeStatus(AccountStatus target, LocalDateTime now) {
        switch (Objects.requireNonNull(target, "El estado es obligatorio")) {
            case ACTIVE -> activate(now);
            case INACTIVE -> inactivate(now);
            case CANCELLED -> cancel(now);
        }
    }

    private void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("La cuenta " + accountNumber + " no está activa");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("El monto debe ser mayor a cero");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("El monto admite máximo dos decimales");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public boolean isGmfExempt() {
        return gmfExempt;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
