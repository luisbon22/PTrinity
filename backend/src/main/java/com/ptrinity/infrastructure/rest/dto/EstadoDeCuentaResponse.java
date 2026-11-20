package com.ptrinity.infrastructure.rest.dto;

import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.EstadoDeCuenta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EstadoDeCuentaResponse(
		CuentaResponse cuenta,
		String titular,
		String identificacionTitular,
		LocalDate desde,
		LocalDate hasta,
		BigDecimal totalCreditos,
		BigDecimal totalDebitos,
		List<MovimientoResponse> movimientos) {

	public static EstadoDeCuentaResponse de(EstadoDeCuenta e) {
		Cliente c = e.cliente();
		return new EstadoDeCuentaResponse(CuentaResponse.de(e.cuenta()), c.getNombres() + " " + c.getApellido(),
				c.getTipoIdentificacion() + " " + c.getNumeroIdentificacion(), e.desde(), e.hasta(), e.totalCreditos(),
				e.totalDebitos(), e.movimientos().stream().map(MovimientoResponse::de).toList());
	}
}
