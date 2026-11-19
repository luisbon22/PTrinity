package com.ptrinity.infrastructure.persistence.repository;

import com.ptrinity.domain.model.TipoIdentificacion;
import com.ptrinity.infrastructure.persistence.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {

	Optional<ClienteEntity> findByTipoIdentificacionAndNumeroIdentificacion(TipoIdentificacion tipo, String numero);
}
