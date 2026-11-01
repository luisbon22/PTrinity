package com.ptrinity.bank.infrastructure.adapter.out.persistence;

import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.model.IdentificationType;
import com.ptrinity.bank.domain.port.out.CustomerRepositoryPort;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.mapper.CustomerPersistenceMapper;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.repository.CustomerJpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida que implementa el puerto de clientes con Spring Data JPA.
 */
@Component
public class CustomerPersistenceAdapter implements CustomerRepositoryPort {

    private final CustomerJpaRepository repository;

    public CustomerPersistenceAdapter(CustomerJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = customer.getId() == null
                ? new CustomerEntity()
                : repository.findById(customer.getId()).orElseGet(CustomerEntity::new);
        CustomerPersistenceMapper.copyToEntity(customer, entity);
        return CustomerPersistenceMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return repository.findById(id).map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public List<Customer> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(CustomerPersistenceMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByIdentification(IdentificationType type, String number, Long excludeId) {
        return excludeId == null
                ? repository.existsByIdentificationTypeAndIdentificationNumber(type, number)
                : repository.existsByIdentificationTypeAndIdentificationNumberAndIdNot(type, number, excludeId);
    }

    @Override
    public boolean existsByEmail(String email, Long excludeId) {
        return excludeId == null
                ? repository.existsByEmailIgnoreCase(email)
                : repository.existsByEmailIgnoreCaseAndIdNot(email, excludeId);
    }
}
