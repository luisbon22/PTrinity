package com.ptrinity.bank.infrastructure.adapter.out.persistence.repository;

import com.ptrinity.bank.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, Long> {

    @Query("select t from TransactionEntity t join fetch t.account a where a.id = :accountId "
            + "order by t.createdAt desc, t.id desc")
    List<TransactionEntity> findByAccountId(@Param("accountId") Long accountId);

    @Query("select t from TransactionEntity t join fetch t.account order by t.createdAt desc, t.id desc")
    List<TransactionEntity> findAllWithAccount();
}
