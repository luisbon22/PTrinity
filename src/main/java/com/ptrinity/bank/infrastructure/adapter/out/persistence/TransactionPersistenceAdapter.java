package com.ptrinity.bank.infrastructure.adapter.out.persistence;

import com.ptrinity.bank.domain.model.Transaction;
import com.ptrinity.bank.domain.port.out.TransactionRepositoryPort;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.mapper.TransactionPersistenceMapper;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.repository.AccountJpaRepository;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.repository.TransactionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adaptador de salida que implementa el puerto de movimientos con Spring Data JPA.
 */
@Component
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository repository;
    private final AccountJpaRepository accountRepository;

    public TransactionPersistenceAdapter(TransactionJpaRepository repository, AccountJpaRepository accountRepository) {
        this.repository = repository;
        this.accountRepository = accountRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        AccountEntity account = accountRepository.getReferenceById(transaction.accountId());
        TransactionEntity saved = repository.save(TransactionPersistenceMapper.toEntity(transaction, account));
        return new Transaction(saved.getId(), transaction.reference(), transaction.type(), transaction.movement(),
                transaction.accountId(), transaction.accountNumber(), transaction.amount(), transaction.gmfAmount(),
                transaction.balanceAfter(), transaction.counterpartAccountNumber(), transaction.description(),
                transaction.createdAt());
    }

    @Override
    public List<Transaction> findByAccountId(Long accountId) {
        return repository.findByAccountId(accountId).stream().map(TransactionPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Transaction> findAll() {
        return repository.findAllWithAccount().stream().map(TransactionPersistenceMapper::toDomain).toList();
    }
}
