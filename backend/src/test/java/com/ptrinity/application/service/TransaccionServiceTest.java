package com.ptrinity.application.service;

import com.ptrinity.domain.exception.RecursoNoEncontradoException;
import com.ptrinity.domain.exception.ReglaNegocioException;
import com.ptrinity.domain.model.Cuenta;
import com.ptrinity.domain.model.EstadoCuenta;
import com.ptrinity.domain.model.Movimiento;
import com.ptrinity.domain.model.Naturaleza;
import com.ptrinity.domain.model.TipoCuenta;
import com.ptrinity.domain.model.TipoTransaccion;
import com.ptrinity.domain.port.out.CuentaRepositoryPort;
import com.ptrinity.domain.port.out.MovimientoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransaccionServiceTest {

	private final Clock reloj = Clock.fixed(Instant.parse("2026-01-15T15:00:00Z"), ZoneId.of("America/Bogota"));
	private final LocalDateTime antes = LocalDateTime.of(2025, 1, 1, 8, 0);

	@Mock
	private CuentaRepositoryPort cuentas;
	@Mock
	private MovimientoRepositoryPort movimientos;

	private TransaccionService service;

	@BeforeEach
	void setUp() {
		service = new TransaccionService(cuentas, movimientos, reloj);
		lenient().when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));
		lenient().when(movimientos.guardar(any())).thenAnswer(i -> i.getArgument(0));
	}

	private Cuenta cuenta(Long id, TipoCuenta tipo, String numero, String saldo, boolean exenta) {
		Cuenta c = Cuenta.reconstruir(id, tipo, numero, EstadoCuenta.ACTIVA, new BigDecimal(saldo), exenta, 1L, antes, antes);
		when(cuentas.buscarPorIdParaActualizar(id)).thenReturn(Optional.of(c));
		return c;
	}

	@Test
	void consignarAumentaSaldoYRegistraCredito() {
		Cuenta c = cuenta(1L, TipoCuenta.AHORROS, "5300000001", "100.00", false);

		Movimiento mov = service.consignar(1L, new BigDecimal("50"), "Deposito");

		assertThat(c.getSaldo()).isEqualByComparingTo("150");
		assertThat(mov.tipo()).isEqualTo(TipoTransaccion.CONSIGNACION);
		assertThat(mov.naturaleza()).isEqualTo(Naturaleza.CREDITO);
		assertThat(mov.saldoResultante()).isEqualByComparingTo("150");
		assertThat(mov.gmf()).isZero();
		assertThat(mov.fecha()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 0));
		assertThat(mov.referencia()).hasSize(36);
	}

	@Test
	void retirarDescuentaMontoYGmf() {
		Cuenta c = cuenta(1L, TipoCuenta.AHORROS, "5300000001", "1000.00", false);

		Movimiento mov = service.retirar(1L, new BigDecimal("500"), null);

		assertThat(c.getSaldo()).isEqualByComparingTo("498");
		assertThat(mov.gmf()).isEqualByComparingTo("2");
		assertThat(mov.naturaleza()).isEqualTo(Naturaleza.DEBITO);
	}

	@Test
	void retiroSinFondosNoGuardaNada() {
		cuenta(1L, TipoCuenta.AHORROS, "5300000001", "10.00", true);

		assertThatThrownBy(() -> service.retirar(1L, new BigDecimal("11"), null))
				.isInstanceOf(ReglaNegocioException.class);
		verify(cuentas, never()).guardar(any());
		verify(movimientos, never()).guardar(any());
	}

	@Test
	void transferirGeneraDebitoYCreditoConLaMismaReferencia() {
		Cuenta origen = cuenta(2L, TipoCuenta.AHORROS, "5300000002", "1000.00", false);
		Cuenta destino = cuenta(1L, TipoCuenta.CORRIENTE, "3300000001", "0.00", false);

		List<Movimiento> resultado = service.transferir(2L, 1L, new BigDecimal("100"), "Pago");

		Movimiento debito = resultado.get(0);
		Movimiento credito = resultado.get(1);
		assertThat(origen.getSaldo()).isEqualByComparingTo("899.60");
		assertThat(destino.getSaldo()).isEqualByComparingTo("100");
		assertThat(debito.naturaleza()).isEqualTo(Naturaleza.DEBITO);
		assertThat(debito.cuentaId()).isEqualTo(2L);
		assertThat(debito.cuentaContraparte()).isEqualTo("3300000001");
		assertThat(credito.naturaleza()).isEqualTo(Naturaleza.CREDITO);
		assertThat(credito.cuentaId()).isEqualTo(1L);
		assertThat(credito.cuentaContraparte()).isEqualTo("5300000002");
		assertThat(credito.gmf()).isZero();
		assertThat(debito.referencia()).isEqualTo(credito.referencia());
	}

	@Test
	void transferirBloqueaLasCuentasEnOrdenAscendente() {
		cuenta(1L, TipoCuenta.CORRIENTE, "3300000001", "500.00", true);
		cuenta(2L, TipoCuenta.AHORROS, "5300000002", "0.00", true);

		service.transferir(1L, 2L, new BigDecimal("100"), null);

		InOrder orden = inOrder(cuentas);
		orden.verify(cuentas).buscarPorIdParaActualizar(1L);
		orden.verify(cuentas).buscarPorIdParaActualizar(2L);
	}

	@Test
	void transferenciaFallidaNoMueveSaldos() {
		Cuenta origen = cuenta(1L, TipoCuenta.AHORROS, "5300000001", "50.00", true);
		Cuenta destino = cuenta(2L, TipoCuenta.AHORROS, "5300000002", "0.00", true);

		assertThatThrownBy(() -> service.transferir(1L, 2L, new BigDecimal("60"), null))
				.hasMessageContaining("Saldo insuficiente");
		assertThat(origen.getSaldo()).isEqualByComparingTo("50");
		assertThat(destino.getSaldo()).isZero();
		verify(movimientos, never()).guardar(any());
	}

	@Test
	void noSeTransfiereALaMismaCuenta() {
		assertThatThrownBy(() -> service.transferir(1L, 1L, BigDecimal.TEN, null)).hasMessageContaining("diferentes");
	}

	@Test
	void transferirACuentaInexistente() {
		cuenta(1L, TipoCuenta.AHORROS, "5300000001", "50.00", true);
		when(cuentas.buscarPorIdParaActualizar(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.transferir(1L, 7L, BigDecimal.TEN, null))
				.isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void obtenerYListarMovimientos() {
		Movimiento mov = new Movimiento(3L, 1L, TipoTransaccion.RETIRO, Naturaleza.DEBITO, BigDecimal.ONE,
				BigDecimal.ZERO, BigDecimal.ZERO, "r", null, null, antes);
		when(movimientos.buscarPorId(3L)).thenReturn(Optional.of(mov));
		when(cuentas.buscarPorId(1L)).thenReturn(Optional.of(
				Cuenta.reconstruir(1L, TipoCuenta.AHORROS, "5300000001", EstadoCuenta.ACTIVA, BigDecimal.ZERO, false, 1L,
						antes, antes)));
		when(movimientos.listarPorCuenta(1L)).thenReturn(List.of(mov));

		assertThat(service.obtener(3L)).isEqualTo(mov);
		assertThat(service.listarPorCuenta(1L)).containsExactly(mov);
	}

	@Test
	void movimientoOCuentaInexistentes() {
		when(movimientos.buscarPorId(9L)).thenReturn(Optional.empty());
		when(cuentas.buscarPorId(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.obtener(9L)).isInstanceOf(RecursoNoEncontradoException.class);
		assertThatThrownBy(() -> service.listarPorCuenta(9L)).isInstanceOf(RecursoNoEncontradoException.class);
	}
}
