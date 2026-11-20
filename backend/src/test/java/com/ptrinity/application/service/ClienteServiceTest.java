package com.ptrinity.application.service;

import com.ptrinity.domain.exception.RecursoDuplicadoException;
import com.ptrinity.domain.exception.RecursoNoEncontradoException;
import com.ptrinity.domain.exception.ReglaNegocioException;
import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.DatosCliente;
import com.ptrinity.domain.model.TipoIdentificacion;
import com.ptrinity.domain.port.out.ClienteRepositoryPort;
import com.ptrinity.domain.port.out.CuentaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

	private final Clock reloj = Clock.fixed(Instant.parse("2026-01-15T15:00:00Z"), ZoneId.of("America/Bogota"));

	@Mock
	private ClienteRepositoryPort clientes;
	@Mock
	private CuentaRepositoryPort cuentas;

	private ClienteService service;

	@BeforeEach
	void setUp() {
		service = new ClienteService(clientes, cuentas, reloj);
	}

	private static DatosCliente datos(String numero) {
		return new DatosCliente(TipoIdentificacion.CC, numero, "Ana", "Gomez", "ana@correo.com", LocalDate.of(1990, 1, 1));
	}

	private static Cliente existente(Long id, String numero) {
		LocalDateTime f = LocalDateTime.of(2025, 1, 1, 8, 0);
		return Cliente.reconstruir(id, datos(numero), f, f);
	}

	@Test
	void crearGuardaElClienteConFechasAutomaticas() {
		when(clientes.buscarPorIdentificacion(TipoIdentificacion.CC, "123")).thenReturn(Optional.empty());
		when(clientes.guardar(any())).thenAnswer(i -> i.getArgument(0));

		Cliente creado = service.crear(datos("123"));

		assertThat(creado.getFechaCreacion()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 0));
		assertThat(creado.getFechaModificacion()).isEqualTo(creado.getFechaCreacion());
	}

	@Test
	void crearRechazaIdentificacionDuplicada() {
		when(clientes.buscarPorIdentificacion(TipoIdentificacion.CC, "123")).thenReturn(Optional.of(existente(1L, "123")));

		assertThatThrownBy(() -> service.crear(datos("123"))).isInstanceOf(RecursoDuplicadoException.class);
		verify(clientes, never()).guardar(any());
	}

	@Test
	void crearRechazaMenoresDeEdadSinTocarElRepositorio() {
		DatosCliente menor = new DatosCliente(TipoIdentificacion.CC, "1", "Ana", "Gomez", "a@b.co", LocalDate.of(2015, 1, 1));

		assertThatThrownBy(() -> service.crear(menor)).isInstanceOf(ReglaNegocioException.class);
		verify(clientes, never()).guardar(any());
	}

	@Test
	void actualizarRecalculaLaFechaDeModificacion() {
		when(clientes.buscarPorId(1L)).thenReturn(Optional.of(existente(1L, "123")));
		when(clientes.buscarPorIdentificacion(TipoIdentificacion.CC, "123")).thenReturn(Optional.of(existente(1L, "123")));
		when(clientes.guardar(any())).thenAnswer(i -> i.getArgument(0));

		Cliente actualizado = service.actualizar(1L, datos("123"));

		assertThat(actualizado.getFechaCreacion()).isEqualTo(LocalDateTime.of(2025, 1, 1, 8, 0));
		assertThat(actualizado.getFechaModificacion()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 0));
	}

	@Test
	void actualizarRechazaIdentificacionDeOtroCliente() {
		when(clientes.buscarPorId(1L)).thenReturn(Optional.of(existente(1L, "123")));
		when(clientes.buscarPorIdentificacion(TipoIdentificacion.CC, "999")).thenReturn(Optional.of(existente(2L, "999")));

		assertThatThrownBy(() -> service.actualizar(1L, datos("999"))).isInstanceOf(RecursoDuplicadoException.class);
	}

	@Test
	void obtenerClienteInexistente() {
		when(clientes.buscarPorId(9L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.obtener(9L)).isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void listarDelegaEnElRepositorio() {
		when(clientes.listar()).thenReturn(List.of(existente(1L, "1")));
		assertThat(service.listar()).hasSize(1);
	}

	@Test
	void eliminarClienteSinProductos() {
		when(clientes.buscarPorId(1L)).thenReturn(Optional.of(existente(1L, "123")));
		when(cuentas.existePorCliente(1L)).thenReturn(false);

		service.eliminar(1L);

		verify(clientes).eliminar(1L);
	}

	@Test
	void noSeEliminaUnClienteConProductos() {
		when(clientes.buscarPorId(1L)).thenReturn(Optional.of(existente(1L, "123")));
		when(cuentas.existePorCliente(1L)).thenReturn(true);

		assertThatThrownBy(() -> service.eliminar(1L)).hasMessageContaining("productos vinculados");
		verify(clientes, never()).eliminar(any());
	}
}
