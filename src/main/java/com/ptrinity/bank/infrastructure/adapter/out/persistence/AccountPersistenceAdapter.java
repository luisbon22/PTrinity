package com.ptrinity.bank.infrastructure.adapter.out.persistence;

import com.ptrinity.bank.domain.model.Account;
import com.ptrinity.bank.domain.port.out.AccountRepositoryPort;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.mapper.AccountPersistenceMapper;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.repository.AccountJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida que implementa el puerto de cuentas con Spring Data JPA.
 */
@Component
public class AccountPersistenceAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository repository;

    public AccountPersistenceAdapter(AccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = account.getId() == null
                ? new AccountEntity()
                : repository.findById(account.getId()).orElseGet(AccountEntity::new);
        AccountPersistenceMapper.copyToEntity(account, entity);
        return AccountPersistenceMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Account> findById(Long id) {
        return repository.findById(id).map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumberForUpdate(String accountNumber) {
        return repository.findByAccountNumberForUpdate(accountNumber).map(AccountPersistenceMapper::toDomain);
    }

    @Override
    public List<Account> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(AccountPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Account> findByCustomerId(Long customerId) {
        return repository.findByCustomerIdOrderByIdAsc(customerId).stream()
                .map(AccountPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCustomerId(Long customerId) {
        return repository.existsByCustomerId(customerId);
    }
}
