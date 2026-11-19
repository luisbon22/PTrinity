package com.ptrinity.domain.port.out;

import com.ptrinity.domain.model.Movimiento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MovimientoRepositoryPort {

	Movimiento guardar(Movimiento movimiento);

	Optional<Movimiento> buscarPorId(Long id);

	List<Movimiento> listarPorCuenta(Long cuentaId);

	/** Movimientos con fecha en [desde, hasta), ordenados del mas antiguo al mas reciente. */
	List<Movimiento> listarPorCuentaEntre(Long cuentaId, LocalDateTime desde, LocalDateTime hasta);
}
