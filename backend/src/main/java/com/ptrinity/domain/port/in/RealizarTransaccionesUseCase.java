package com.ptrinity.domain.port.in;

import com.ptrinity.domain.model.Movimiento;

import java.math.BigDecimal;
import java.util.List;

public interface RealizarTransaccionesUseCase {

	Movimiento consignar(Long cuentaId, BigDecimal monto, String descripcion);

	Movimiento retirar(Long cuentaId, BigDecimal monto, String descripcion);

	/** Devuelve el debito de la cuenta origen y el credito de la cuenta destino. */
	List<Movimiento> transferir(Long cuentaOrigenId, Long cuentaDestinoId, BigDecimal monto, String descripcion);

	Movimiento obtener(Long id);

	List<Movimiento> listarPorCuenta(Long cuentaId);
}
