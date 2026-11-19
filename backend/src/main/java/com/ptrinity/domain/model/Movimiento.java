package com.ptrinity.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Asiento inmutable sobre una cuenta. Una transferencia genera dos movimientos
 * (debito y credito) que comparten la misma referencia.
 */
public record Movimiento(
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

	public static Movimiento nuevo(Cuenta cuenta, TipoTransaccion tipo, Naturaleza naturaleza, BigDecimal monto,
			BigDecimal gmf, String referencia, String cuentaContraparte, String descripcion) {
		return new Movimiento(null, cuenta.getId(), tipo, naturaleza, monto.setScale(2), gmf, cuenta.getSaldo(),
				referencia, cuentaContraparte, descripcion, cuenta.getFechaModificacion());
	}
}
