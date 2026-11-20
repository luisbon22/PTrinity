package com.ptrinity.application.service;

import com.ptrinity.domain.exception.RecursoNoEncontradoException;
import com.ptrinity.domain.exception.ReglaNegocioException;
import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.Cuenta;
import com.ptrinity.domain.model.DatosCliente;
import com.ptrinity.domain.model.EstadoCuenta;
import com.ptrinity.domain.model.EstadoDeCuenta;
import com.ptrinity.domain.model.Movimiento;
import com.ptrinity.domain.model.Naturaleza;
import com.ptrinity.domain.model.TipoCuenta;
import com.ptrinity.domain.model.TipoIdentificacion;
import com.ptrinity.domain.model.TipoTransaccion;
import com.ptrinity.domain.port.out.ClienteRepositoryPort;
import com.ptrinity.domain.port.out.CuentaRepositoryPort;
import com.ptrinity.domain.port.out.GeneradorNumeroCuentaPort;
import com.ptrinity.domain.port.out.MovimientoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

	private final Clock reloj = Clock.fixed(Instant.parse("2026-01-15T15:00:00Z"), ZoneId.of("America/Bogota"));
	private final LocalDateTime antes = LocalDateTime.of(2025, 1, 1, 8, 0);

	@Mock
	private CuentaRepositoryPort cuentas;
	@Mock
	private ClienteRepositoryPort clientes;
	@Mock
	private MovimientoRepositoryPort movimientos;
	@Mock
	private GeneradorNumeroCuentaPort generador;

	private CuentaService service;

	@BeforeEach
	void setUp() {
		service = new CuentaService(cuentas, clientes, movimientos, generador, reloj);
	}

	private Cliente cliente() {
		return Cliente.reconstruir(1L, new DatosCliente(TipoIdentificacion.CC, "1", "Ana", "Gomez", "a@b.co",
				LocalDate.of(1990, 1, 1)), antes, antes);
	}

	private Cuenta cuenta(String saldo, EstadoCuenta estado) {
		return Cuenta.reconstruir(5L, TipoCuenta.AHORROS, "5300000005", estado, new BigDecimal(saldo), false, 1L,
				antes, antes);
	}

	@Test
	void abrirCuentaDeAhorrosActivaConNumeroUnico() {
		when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente()));
		when(generador.generar(TipoCuenta.AHORROS)).thenReturn("5300000001", "5300000002");
		when(cuentas.existePorNumero("5300000001")).thenReturn(true);
		when(cuentas.existePorNumero("5300000002")).thenReturn(false);
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));

		Cuenta cuenta = service.abrir(1L, TipoCuenta.AHORROS, true);

		assertThat(cuenta.getNumero()).isEqualTo("5300000002");
		assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
		assertThat(cuenta.isExentaGmf()).isTrue();
		assertThat(cuenta.getFechaCreacion()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 0));
	}

	@Test
	void abrirFallaSiNoHayNumeroDisponible() {
		when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente()));
		when(generador.generar(TipoCuenta.CORRIENTE)).thenReturn("3300000001");
		when(cuentas.existePorNumero("3300000001")).thenReturn(true);

		assertThatThrownBy(() -> service.abrir(1L, TipoCuenta.CORRIENTE, false)).isInstanceOf(IllegalStateException.class);
		verify(generador, times(CuentaService.INTENTOS_NUMERO_UNICO)).generar(TipoCuenta.CORRIENTE);
	}

	@Test
	void noSeAbreCuentaParaClienteInexistente() {
		when(clientes.buscarPorId(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.abrir(1L, TipoCuenta.AHORROS, false))
				.isInstanceOf(RecursoNoEncontradoException.class);
		verify(cuentas, never()).guardar(any());
	}

	@Test
	void obtenerYListar() {
		when(cuentas.buscarPorId(5L)).thenReturn(Optional.of(cuenta("0", EstadoCuenta.ACTIVA)));
		when(cuentas.listar()).thenReturn(List.of(cuenta("0", EstadoCuenta.ACTIVA)));
		when(cuentas.listarPorCliente(1L)).thenReturn(List.of());

		assertThat(service.obtener(5L).getId()).isEqualTo(5L);
		assertThat(service.listar(null)).hasSize(1);
		assertThat(service.listar(1L)).isEmpty();
	}

	@Test
	void obtenerCuentaInexistente() {
		when(cuentas.buscarPorId(9L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.obtener(9L)).isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void actualizarExencionGmf() {
		when(cuentas.buscarPorIdParaActualizar(5L)).thenReturn(Optional.of(cuenta("0", EstadoCuenta.ACTIVA)));
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));

		Cuenta cuenta = service.actualizarExencionGmf(5L, true);

		assertThat(cuenta.isExentaGmf()).isTrue();
		assertThat(cuenta.getFechaModificacion()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 0));
	}

	@Test
	void activarInactivarYCancelar() {
		when(cuentas.buscarPorIdParaActualizar(5L)).thenReturn(Optional.of(cuenta("0", EstadoCuenta.ACTIVA)));
		when(cuentas.guardar(any())).thenAnswer(i -> i.getArgument(0));

		assertThat(service.cambiarEstado(5L, EstadoCuenta.INACTIVA).getEstado()).isEqualTo(EstadoCuenta.INACTIVA);
		assertThat(service.cambiarEstado(5L, EstadoCuenta.ACTIVA).getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
		assertThat(service.cambiarEstado(5L, EstadoCuenta.CANCELADA).getEstado()).isEqualTo(EstadoCuenta.CANCELADA);
	}

	@Test
	void noSeCancelaConSaldo() {
		when(cuentas.buscarPorIdParaActualizar(5L)).thenReturn(Optional.of(cuenta("10", EstadoCuenta.ACTIVA)));

		assertThatThrownBy(() -> service.cambiarEstado(5L, EstadoCuenta.CANCELADA))
				.isInstanceOf(ReglaNegocioException.class);
		verify(cuentas, never()).guardar(any());
	}

	@Test
	void cambiarEstadoDeCuentaInexistente() {
		when(cuentas.buscarPorIdParaActualizar(9L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.cambiarEstado(9L, EstadoCuenta.ACTIVA))
				.isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void estadoDeCuentaEnUnRango() {
		when(cuentas.buscarPorId(5L)).thenReturn(Optional.of(cuenta("100", EstadoCuenta.ACTIVA)));
		when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente()));
		Movimiento mov = new Movimiento(1L, 5L, TipoTransaccion.CONSIGNACION, Naturaleza.CREDITO,
				new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"), "r", null, null, antes);
		when(movimientos.listarPorCuentaEntre(5L, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 11, 0, 0)))
				.thenReturn(List.of(mov));

		EstadoDeCuenta extracto = service.estadoDeCuenta(5L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10));

		assertThat(extracto.totalCreditos()).isEqualByComparingTo("100");
		assertThat(extracto.cliente().getId()).isEqualTo(1L);
	}

	@Test
	void estadoDeCuentaPorDefectoEsElMesEnCurso() {
		when(cuentas.buscarPorId(5L)).thenReturn(Optional.of(cuenta("0", EstadoCuenta.ACTIVA)));
		when(clientes.buscarPorId(1L)).thenReturn(Optional.of(cliente()));
		when(movimientos.listarPorCuentaEntre(5L, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 16, 0, 0)))
				.thenReturn(List.of());

		EstadoDeCuenta extracto = service.estadoDeCuenta(5L, null, null);

		assertThat(extracto.desde()).isEqualTo(LocalDate.of(2026, 1, 1));
		assertThat(extracto.hasta()).isEqualTo(LocalDate.of(2026, 1, 15));
	}

	@Test
	void estadoDeCuentaConRangoInvertido() {
		when(cuentas.buscarPorId(5L)).thenReturn(Optional.of(cuenta("0", EstadoCuenta.ACTIVA)));

		assertThatThrownBy(() -> service.estadoDeCuenta(5L, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
				.hasMessageContaining("fecha inicial");
	}
}
