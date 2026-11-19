package com.ptrinity.infrastructure.persistence.repository;

import com.ptrinity.infrastructure.persistence.entity.CuentaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CuentaJpaRepository extends JpaRepository<CuentaEntity, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from CuentaEntity c where c.id = :id")
	Optional<CuentaEntity> findByIdForUpdate(Long id);

	List<CuentaEntity> findByClienteIdOrderByIdAsc(Long clienteId);

	boolean existsByNumero(String numero);

	boolean existsByClienteId(Long clienteId);
}
