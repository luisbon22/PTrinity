package com.ptrinity.domain.model;

import com.ptrinity.domain.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuentaTest {

	private final Clock reloj = Clock.fixed(Instant.parse("2026-01-15T15:00:00Z"), ZoneId.of("America/Bogota"));

	private Cuenta ahorros(boolean exenta) {
		return Cuenta.abrir(TipoCuenta.AHORROS, "5300000001", exenta, 1L, reloj);
	}

	private Cuenta corriente() {
		return Cuenta.abrir(TipoCuenta.CORRIENTE, "3300000001", true, 1L, reloj);
	}

	private static BigDecimal pesos(String valor) {
		return new BigDecimal(valor);
	}

	@Test
	void abrirCreaCuentaActivaConSaldoCero() {
		Cuenta cuenta = ahorros(false);

		assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
		assertThat(cuenta.getSaldo()).isEqualByComparingTo("0");
		assertThat(cuenta.getFechaCreacion()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 0));
		assertThat(cuenta.getFechaModificacion()).isEqualTo(cuenta.getFechaCreacion());
		assertThat(cuenta.getClienteId()).isEqualTo(1L);
		assertThat(cuenta.getTipo()).isEqualTo(TipoCuenta.AHORROS);
		assertThat(cuenta.isExentaGmf()).isFalse();
	}

	@Test
	void abrirValidaTipoClienteYNumero() {
		assertThatThrownBy(() -> Cuenta.abrir(null, "5300000001", false, 1L, reloj)).hasMessageContaining("tipo");
		assertThatThrownBy(() -> Cuenta.abrir(TipoCuenta.AHORROS, "5300000001", false, null, reloj))
				.hasMessageContaining("cliente");
		assertThatThrownBy(() -> Cuenta.abrir(TipoCuenta.AHORROS, "3300000001", false, 1L, reloj))
				.hasMessageContaining("iniciar en 53");
		assertThatThrownBy(() -> Cuenta.abrir(TipoCuenta.CORRIENTE, "33000001", false, 1L, reloj))
				.hasMessageContaining("10 digitos");
		assertThatThrownBy(() -> Cuenta.abrir(TipoCuenta.CORRIENTE, null, false, 1L, reloj))
				.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void acreditarSumaAlSaldo() {
		Cuenta cuenta = ahorros(true);
		cuenta.acreditar(pesos("1000.5"), reloj);
		assertThat(cuenta.getSaldo()).isEqualTo(pesos("1000.50"));
	}

	@Test
	void debitarCobraGmfSiNoEsExenta() {
		Cuenta cuenta = ahorros(false);
		cuenta.acreditar(pesos("100000"), reloj);

		BigDecimal gmf = cuenta.debitar(pesos("50000"), reloj);

		assertThat(gmf).isEqualTo(pesos("200.00"));
		assertThat(cuenta.getSaldo()).isEqualTo(pesos("49800.00"));
	}

	@Test
	void debitarSinGmfSiEsExenta() {
		Cuenta cuenta = ahorros(true);
		cuenta.acreditar(pesos("100"), reloj);

		assertThat(cuenta.debitar(pesos("100"), reloj)).isZero();
		assertThat(cuenta.getSaldo()).isZero();
	}

	@Test
	void ahorrosNoPuedeQuedarEnNegativo() {
		Cuenta cuenta = ahorros(false);
		cuenta.acreditar(pesos("100"), reloj);

		// 100 mas GMF de 0.40 supera el saldo
		assertThatThrownBy(() -> cuenta.debitar(pesos("100"), reloj)).hasMessageContaining("Saldo insuficiente");
		assertThat(cuenta.getSaldo()).isEqualTo(pesos("100.00"));
	}

	@Test
	void corrienteUsaCupoDeSobregiro() {
		Cuenta cuenta = corriente();
		assertThat(cuenta.saldoDisponible()).isEqualTo(Cuenta.CUPO_SOBREGIRO_CORRIENTE);

		cuenta.debitar(pesos("1000000"), reloj);

		assertThat(cuenta.getSaldo()).isEqualTo(pesos("-1000000.00"));
		assertThat(cuenta.saldoDisponible()).isZero();
		assertThatThrownBy(() -> cuenta.debitar(pesos("0.01"), reloj)).hasMessageContaining("Saldo insuficiente");
	}

	@Test
	void montoInvalido() {
		Cuenta cuenta = ahorros(true);
		assertThatThrownBy(() -> cuenta.acreditar(null, reloj)).hasMessageContaining("mayor a cero");
		assertThatThrownBy(() -> cuenta.acreditar(BigDecimal.ZERO, reloj)).hasMessageContaining("mayor a cero");
		assertThatThrownBy(() -> cuenta.acreditar(pesos("-5"), reloj)).hasMessageContaining("mayor a cero");
		assertThatThrownBy(() -> cuenta.acreditar(pesos("1.001"), reloj)).hasMessageContaining("dos decimales");
	}

	@Test
	void cuentaInactivaNoAceptaMovimientos() {
		Cuenta cuenta = ahorros(true);
		cuenta.inactivar(reloj);

		assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.INACTIVA);
		assertThatThrownBy(() -> cuenta.acreditar(pesos("10"), reloj)).hasMessageContaining("no esta activa");

		cuenta.activar(reloj);
		cuenta.acreditar(pesos("10"), reloj);
		assertThat(cuenta.getSaldo()).isEqualTo(pesos("10.00"));
	}

	@Test
	void soloSeCancelaConSaldoCero() {
		Cuenta cuenta = ahorros(true);
		cuenta.acreditar(pesos("10"), reloj);
		assertThatThrownBy(() -> cuenta.cancelar(reloj)).hasMessageContaining("saldo igual a $0");

		cuenta.debitar(pesos("10"), reloj);
		cuenta.cancelar(reloj);
		assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.CANCELADA);
	}

	@Test
	void cuentaCanceladaEsEstadoFinal() {
		Cuenta cuenta = ahorros(true);
		cuenta.cancelar(reloj);

		assertThatThrownBy(() -> cuenta.activar(reloj)).hasMessageContaining("cancelada");
		assertThatThrownBy(() -> cuenta.inactivar(reloj)).hasMessageContaining("cancelada");
		assertThatThrownBy(() -> cuenta.cancelar(reloj)).hasMessageContaining("cancelada");
		assertThatThrownBy(() -> cuenta.cambiarExencionGmf(false, reloj)).hasMessageContaining("cancelada");
	}

	@Test
	void cambiarExencionGmf() {
		Cuenta cuenta = ahorros(false);
		cuenta.cambiarExencionGmf(true, reloj);
		assertThat(cuenta.isExentaGmf()).isTrue();
	}

	@Test
	void reconstruirConservaTodo() {
		LocalDateTime f = LocalDateTime.of(2025, 1, 1, 0, 0);
		Cuenta cuenta = Cuenta.reconstruir(3L, TipoCuenta.CORRIENTE, "3312345678", EstadoCuenta.INACTIVA,
				pesos("5.00"), true, 2L, f, f);

		assertThat(cuenta.getId()).isEqualTo(3L);
		assertThat(cuenta.getNumero()).isEqualTo("3312345678");
		assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.INACTIVA);
		assertThat(cuenta.getFechaCreacion()).isEqualTo(f);
	}
}
