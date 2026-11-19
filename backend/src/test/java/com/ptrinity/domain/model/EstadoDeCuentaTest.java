package com.ptrinity.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoDeCuentaTest {

	private static Movimiento mov(Naturaleza naturaleza, String monto, String gmf) {
		return new Movimiento(1L, 1L, TipoTransaccion.RETIRO, naturaleza, new BigDecimal(monto), new BigDecimal(gmf),
				BigDecimal.ZERO, "ref", null, null, LocalDateTime.now());
	}

	@Test
	void totalizaCreditosYDebitosConGmf() {
		EstadoDeCuenta extracto = EstadoDeCuenta.de(null, null, LocalDate.MIN, LocalDate.MAX, List.of(
				mov(Naturaleza.CREDITO, "1000.00", "0.00"),
				mov(Naturaleza.DEBITO, "200.00", "0.80"),
				mov(Naturaleza.CREDITO, "50.00", "0.00")));

		assertThat(extracto.totalCreditos()).isEqualTo(new BigDecimal("1050.00"));
		assertThat(extracto.totalDebitos()).isEqualTo(new BigDecimal("200.80"));
		assertThat(extracto.movimientos()).hasSize(3);
	}

	@Test
	void sinMovimientosLosTotalesSonCero() {
		EstadoDeCuenta extracto = EstadoDeCuenta.de(null, null, LocalDate.MIN, LocalDate.MAX, List.of());
		assertThat(extracto.totalCreditos()).isZero();
		assertThat(extracto.totalDebitos()).isZero();
	}
}
