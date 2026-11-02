package com.ptrinity.bank.application.service;

import com.ptrinity.bank.domain.exception.BusinessRuleException;
import com.ptrinity.bank.domain.exception.DuplicateResourceException;
import com.ptrinity.bank.domain.exception.ResourceNotFoundException;
import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.model.CustomerData;
import com.ptrinity.bank.domain.port.in.CustomerUseCase;
import com.ptrinity.bank.domain.port.out.AccountRepositoryPort;
import com.ptrinity.bank.domain.port.out.CustomerRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementa los casos de uso de clientes orquestando el dominio y los puertos de salida.
 */
@Service
@Transactional
public class CustomerService implements CustomerUseCase {

    private final CustomerRepositoryPort customerRepository;
    private final AccountRepositoryPort accountRepository;
    private final Clock clock;

    public CustomerService(CustomerRepositoryPort customerRepository, AccountRepositoryPort accountRepository,
                           Clock clock) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Override
    public Customer create(CustomerData data) {
        Customer customer = Customer.create(data, LocalDateTime.now(clock));
        ensureUnique(customer, null);
        return customerRepository.save(customer);
    }

    @Override
    public Customer update(Long id, CustomerData data) {
        Customer customer = findById(id);
        customer.update(data, LocalDateTime.now(clock));
        ensureUnique(customer, id);
        return customerRepository.save(customer);
    }

    @Override
    public void delete(Long id) {
        findById(id);
        if (accountRepository.existsByCustomerId(id)) {
            throw new BusinessRuleException("No se puede eliminar el cliente porque tiene productos vinculados");
        }
        customerRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    private void ensureUnique(Customer customer, Long excludeId) {
        if (customerRepository.existsByIdentification(customer.getIdentificationType(),
                customer.getIdentificationNumber(), excludeId)) {
            throw new DuplicateResourceException("Ya existe un cliente con la identificación "
                    + customer.getIdentificationType() + " " + customer.getIdentificationNumber());
        }
        if (customerRepository.existsByEmail(customer.getEmail(), excludeId)) {
            throw new DuplicateResourceException("Ya existe un cliente con el correo " + customer.getEmail());
        }
    }
}
