package com.ptrinity.bank.domain.port.out;

import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.model.IdentificationType;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia de clientes.
 */
public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findById(Long id);

    List<Customer> findAll();

    void deleteById(Long id);

    boolean existsByIdentification(IdentificationType type, String number, Long excludeId);

    boolean existsByEmail(String email, Long excludeId);
}
