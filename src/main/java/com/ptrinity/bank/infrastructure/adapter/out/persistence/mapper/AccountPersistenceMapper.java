package com.ptrinity.bank.infrastructure.adapter.out.persistence.mapper;

import com.ptrinity.bank.domain.model.Account;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.entity.AccountEntity;

public final class AccountPersistenceMapper {

    private AccountPersistenceMapper() {
    }

    public static Account toDomain(AccountEntity entity) {
        return new Account(entity.getId(), entity.getAccountType(), entity.getAccountNumber(), entity.getStatus(),
                entity.getBalance(), entity.getOverdraftLimit(), entity.isGmfExempt(), entity.getCustomerId(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static void copyToEntity(Account account, AccountEntity entity) {
        entity.setAccountType(account.getAccountType());
        entity.setAccountNumber(account.getAccountNumber());
        entity.setStatus(account.getStatus());
        entity.setBalance(account.getBalance());
        entity.setOverdraftLimit(account.getOverdraftLimit());
        entity.setGmfExempt(account.isGmfExempt());
        entity.setCustomerId(account.getCustomerId());
        entity.setCreatedAt(account.getCreatedAt());
        entity.setUpdatedAt(account.getUpdatedAt());
    }
}
