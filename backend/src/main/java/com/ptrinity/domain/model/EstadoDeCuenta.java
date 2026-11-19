package com.ptrinity.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Extracto de una cuenta en un rango de fechas. */
public record EstadoDeCuenta(
		Cuenta cuenta,
		Cliente cliente,
		LocalDate desde,
		LocalDate hasta,
		BigDecimal totalCreditos,
		BigDecimal totalDebitos,
		List<Movimiento> movimientos) {

	public static EstadoDeCuenta de(Cuenta cuenta, Cliente cliente, LocalDate desde, LocalDate hasta,
			List<Movimiento> movimientos) {
		BigDecimal creditos = sumar(movimientos, Naturaleza.CREDITO);
		BigDecimal debitos = sumar(movimientos, Naturaleza.DEBITO);
		return new EstadoDeCuenta(cuenta, cliente, desde, hasta, creditos, debitos, List.copyOf(movimientos));
	}

	/** Los debitos incluyen el GMF cobrado. */
	private static BigDecimal sumar(List<Movimiento> movimientos, Naturaleza naturaleza) {
		return movimientos.stream()
				.filter(m -> m.naturaleza() == naturaleza)
				.map(m -> m.monto().add(m.gmf()))
				.reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
	}
}
