package com.ptrinity.domain.port.out;

import com.ptrinity.domain.model.Cuenta;

import java.util.List;
import java.util.Optional;

public interface CuentaRepositoryPort {

	Cuenta guardar(Cuenta cuenta);

	Optional<Cuenta> buscarPorId(Long id);

	/** Bloquea la fila hasta el fin de la transaccion (aislamiento ante saldos concurrentes). */
	Optional<Cuenta> buscarPorIdParaActualizar(Long id);

	List<Cuenta> listar();

	List<Cuenta> listarPorCliente(Long clienteId);

	boolean existePorNumero(String numero);

	boolean existePorCliente(Long clienteId);
}
