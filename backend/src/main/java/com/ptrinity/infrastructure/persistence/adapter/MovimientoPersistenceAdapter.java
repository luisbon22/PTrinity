package com.ptrinity.infrastructure.persistence.adapter;

import com.ptrinity.domain.model.Movimiento;
import com.ptrinity.domain.port.out.MovimientoRepositoryPort;
import com.ptrinity.infrastructure.persistence.repository.MovimientoJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
class MovimientoPersistenceAdapter implements MovimientoRepositoryPort {

	private final MovimientoJpaRepository repository;

	MovimientoPersistenceAdapter(MovimientoJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Movimiento guardar(Movimiento movimiento) {
		return PersistenciaMapper.aDominio(repository.save(PersistenciaMapper.aEntidad(movimiento)));
	}

	@Override
	public Optional<Movimiento> buscarPorId(Long id) {
		return repository.findById(id).map(PersistenciaMapper::aDominio);
	}

	@Override
	public List<Movimiento> listarPorCuenta(Long cuentaId) {
		return repository.findByCuentaIdOrderByFechaDescIdDesc(cuentaId).stream()
				.map(PersistenciaMapper::aDominio).toList();
	}

	@Override
	public List<Movimiento> listarPorCuentaEntre(Long cuentaId, LocalDateTime desde, LocalDateTime hasta) {
		return repository.findByCuentaIdAndFechaGreaterThanEqualAndFechaLessThanOrderByFechaAscIdAsc(cuentaId, desde, hasta)
				.stream().map(PersistenciaMapper::aDominio).toList();
	}
}
