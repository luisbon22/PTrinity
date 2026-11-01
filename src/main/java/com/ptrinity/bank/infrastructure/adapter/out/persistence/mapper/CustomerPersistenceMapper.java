package com.ptrinity.bank.infrastructure.adapter.out.persistence.mapper;

import com.ptrinity.bank.domain.model.Customer;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.entity.CustomerEntity;

public final class CustomerPersistenceMapper {

    private CustomerPersistenceMapper() {
    }

    public static Customer toDomain(CustomerEntity entity) {
        return new Customer(entity.getId(), entity.getIdentificationType(), entity.getIdentificationNumber(),
                entity.getFirstName(), entity.getLastName(), entity.getEmail(), entity.getBirthDate(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static void copyToEntity(Customer customer, CustomerEntity entity) {
        entity.setIdentificationType(customer.getIdentificationType());
        entity.setIdentificationNumber(customer.getIdentificationNumber());
        entity.setFirstName(customer.getFirstName());
        entity.setLastName(customer.getLastName());
        entity.setEmail(customer.getEmail());
        entity.setBirthDate(customer.getBirthDate());
        entity.setCreatedAt(customer.getCreatedAt());
        entity.setUpdatedAt(customer.getUpdatedAt());
    }
}
