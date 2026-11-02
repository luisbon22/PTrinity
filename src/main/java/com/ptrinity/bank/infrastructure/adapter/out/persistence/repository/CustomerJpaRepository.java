package com.ptrinity.bank.infrastructure.adapter.out.persistence.repository;

import com.ptrinity.bank.domain.model.IdentificationType;
import com.ptrinity.bank.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {

    boolean existsByIdentificationTypeAndIdentificationNumber(IdentificationType type, String number);

    boolean existsByIdentificationTypeAndIdentificationNumberAndIdNot(IdentificationType type, String number, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
