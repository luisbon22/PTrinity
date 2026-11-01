package com.ptrinity.bank.domain.port.in;

import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.domain.model.CustomerData;

import java.util.List;

/**
 * Puerto de entrada: casos de uso de administración de clientes.
 */
public interface CustomerUseCase {

    Customer create(CustomerData data);

    Customer update(Long id, CustomerData data);

    void delete(Long id);

    Customer findById(Long id);

    List<Customer> findAll();
}
