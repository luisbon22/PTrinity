package com.ptrinity.infrastructure.persistence.repository;

import com.ptrinity.infrastructure.persistence.entity.MovimientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoJpaRepository extends JpaRepository<MovimientoEntity, Long> {

	List<MovimientoEntity> findByCuentaIdOrderByFechaDescIdDesc(Long cuentaId);

	List<MovimientoEntity> findByCuentaIdAndFechaGreaterThanEqualAndFechaLessThanOrderByFechaAscIdAsc(
			Long cuentaId, LocalDateTime desde, LocalDateTime hasta);
}
