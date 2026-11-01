package com.ptrinity.bank.infrastructure.adapter.out.persistence.entity;

import com.ptrinity.bank.domain.model.MovementType;
import com.ptrinity.bank.domain.model.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36, updatable = false)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 10, updatable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 6, updatable = false)
    private MovementType movementType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private AccountEntity account;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "gmf_amount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal gmfAmount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal balanceAfter;

    @Column(name = "counterpart_account_number", length = 10, updatable = false)
    private String counterpartAccountNumber;

    @Column(length = 255, updatable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public AccountEntity getAccount() {
        return account;
    }

    public void setAccount(AccountEntity account) {
        this.account = account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getGmfAmount() {
        return gmfAmount;
    }

    public void setGmfAmount(BigDecimal gmfAmount) {
        this.gmfAmount = gmfAmount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getCounterpartAccountNumber() {
        return counterpartAccountNumber;
    }

    public void setCounterpartAccountNumber(String counterpartAccountNumber) {
        this.counterpartAccountNumber = counterpartAccountNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
