package com.ptrinity.bank.infrastructure.adapter.out.persistence.repository;

import com.ptrinity.bank.infrastructure.adapter.out.persistence.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.accountNumber = :accountNumber")
    Optional<AccountEntity> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    List<AccountEntity> findByCustomerIdOrderByIdAsc(Long customerId);

    List<AccountEntity> findAllByOrderByIdAsc();

    boolean existsByCustomerId(Long customerId);
}
