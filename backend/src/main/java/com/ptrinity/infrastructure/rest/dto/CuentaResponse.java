package com.ptrinity.infrastructure.rest.dto;

import com.ptrinity.domain.model.Cuenta;
import com.ptrinity.domain.model.EstadoCuenta;
import com.ptrinity.domain.model.TipoCuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CuentaResponse(
		Long id,
		TipoCuenta tipoCuenta,
		String numeroCuenta,
		EstadoCuenta estado,
		BigDecimal saldo,
		BigDecimal saldoDisponible,
		boolean exentaGmf,
		Long clienteId,
		LocalDateTime fechaCreacion,
		LocalDateTime fechaModificacion) {

	public static CuentaResponse de(Cuenta c) {
		return new CuentaResponse(c.getId(), c.getTipo(), c.getNumero(), c.getEstado(), c.getSaldo(), c.saldoDisponible(),
				c.isExentaGmf(), c.getClienteId(), c.getFechaCreacion(), c.getFechaModificacion());
	}
}
