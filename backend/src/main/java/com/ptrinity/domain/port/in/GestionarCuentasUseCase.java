package com.ptrinity.domain.port.in;

import com.ptrinity.domain.model.Cuenta;
import com.ptrinity.domain.model.EstadoCuenta;
import com.ptrinity.domain.model.EstadoDeCuenta;
import com.ptrinity.domain.model.TipoCuenta;

import java.time.LocalDate;
import java.util.List;

public interface GestionarCuentasUseCase {

	Cuenta abrir(Long clienteId, TipoCuenta tipo, boolean exentaGmf);

	Cuenta obtener(Long id);

	/** Con clienteId nulo lista todas las cuentas. */
	List<Cuenta> listar(Long clienteId);

	Cuenta actualizarExencionGmf(Long id, boolean exentaGmf);

	Cuenta cambiarEstado(Long id, EstadoCuenta estado);

	EstadoDeCuenta estadoDeCuenta(Long id, LocalDate desde, LocalDate hasta);
}
