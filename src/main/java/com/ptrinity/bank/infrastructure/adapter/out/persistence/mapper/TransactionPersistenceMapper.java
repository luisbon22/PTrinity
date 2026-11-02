package com.ptrinity.bank.infrastructure.adapter.out.persistence.mapper;

import com.ptrinity.bank.domain.model.Transaction;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.entity.TransactionEntity;

public final class TransactionPersistenceMapper {

    private TransactionPersistenceMapper() {
    }

    public static Transaction toDomain(TransactionEntity entity) {
        AccountEntity account = entity.getAccount();
        return new Transaction(entity.getId(), entity.getReference(), entity.getTransactionType(),
                entity.getMovementType(), account.getId(), account.getAccountNumber(), entity.getAmount(),
                entity.getGmfAmount(), entity.getBalanceAfter(), entity.getCounterpartAccountNumber(),
                entity.getDescription(), entity.getCreatedAt());
    }

    public static TransactionEntity toEntity(Transaction transaction, AccountEntity account) {
        TransactionEntity entity = new TransactionEntity();
        entity.setReference(transaction.reference());
        entity.setTransactionType(transaction.type());
        entity.setMovementType(transaction.movement());
        entity.setAccount(account);
        entity.setAmount(transaction.amount());
        entity.setGmfAmount(transaction.gmfAmount());
        entity.setBalanceAfter(transaction.balanceAfter());
        entity.setCounterpartAccountNumber(transaction.counterpartAccountNumber());
        entity.setDescription(transaction.description());
        entity.setCreatedAt(transaction.createdAt());
        return entity;
    }
}
