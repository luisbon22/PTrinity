package com.ptrinity.infrastructure.rest.dto;

import com.ptrinity.domain.model.Movimiento;
import com.ptrinity.domain.model.Naturaleza;
import com.ptrinity.domain.model.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoResponse(
		Long id,
		Long cuentaId,
		TipoTransaccion tipo,
		Naturaleza naturaleza,
		BigDecimal monto,
		BigDecimal gmf,
		BigDecimal saldoResultante,
		String referencia,
		String cuentaContraparte,
		String descripcion,
		LocalDateTime fecha) {

	public static MovimientoResponse de(Movimiento m) {
		return new MovimientoResponse(m.id(), m.cuentaId(), m.tipo(), m.naturaleza(), m.monto(), m.gmf(),
				m.saldoResultante(), m.referencia(), m.cuentaContraparte(), m.descripcion(), m.fecha());
	}
}
