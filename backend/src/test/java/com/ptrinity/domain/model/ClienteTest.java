package com.ptrinity.domain.model;

import com.ptrinity.domain.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteTest {

	private final Clock reloj = Clock.fixed(Instant.parse("2026-01-15T15:00:00Z"), ZoneId.of("America/Bogota"));

	static DatosCliente datos(LocalDate nacimiento) {
		return new DatosCliente(TipoIdentificacion.CC, " 1020304050 ", " Ana ", "Gomez", "Ana.Gomez@Correo.com", nacimiento);
	}

	@Test
	void registrarCalculaFechasYNormalizaDatos() {
		Cliente cliente = Cliente.registrar(datos(LocalDate.of(1990, 5, 14)), reloj);

		assertThat(cliente.getFechaCreacion()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 0));
		assertThat(cliente.getFechaModificacion()).isEqualTo(cliente.getFechaCreacion());
		assertThat(cliente.getNumeroIdentificacion()).isEqualTo("1020304050");
		assertThat(cliente.getNombres()).isEqualTo("Ana");
		assertThat(cliente.getCorreo()).isEqualTo("ana.gomez@correo.com");
		assertThat(cliente.datos().apellido()).isEqualTo("Gomez");
	}

	@Test
	void actualizarRecalculaSoloLaFechaDeModificacion() {
		Cliente cliente = Cliente.registrar(datos(LocalDate.of(1990, 5, 14)), reloj);
		Clock despues = Clock.offset(reloj, Duration.ofDays(3));

		cliente.actualizar(new DatosCliente(TipoIdentificacion.CE, "99", "Maria", "Lopez", "m@x.co",
				LocalDate.of(1985, 1, 1)), despues);

		assertThat(cliente.getFechaCreacion()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 0));
		assertThat(cliente.getFechaModificacion()).isEqualTo(LocalDateTime.of(2026, 1, 18, 10, 0));
		assertThat(cliente.getTipoIdentificacion()).isEqualTo(TipoIdentificacion.CE);
		assertThat(cliente.getNombres()).isEqualTo("Maria");
	}

	@Test
	void quienCumpleDieciochoHoyEsMayorDeEdad() {
		assertThat(Cliente.registrar(datos(LocalDate.of(2008, 1, 15)), reloj).getFechaNacimiento())
				.isEqualTo(LocalDate.of(2008, 1, 15));
	}

	@Test
	void menorDeEdadNoPuedeRegistrarse() {
		assertThatThrownBy(() -> Cliente.registrar(datos(LocalDate.of(2008, 1, 16)), reloj))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("mayor de edad");
	}

	@Test
	void unClienteNoPuedeQuedarMenorDeEdadAlActualizar() {
		Cliente cliente = Cliente.registrar(datos(LocalDate.of(1990, 5, 14)), reloj);
		assertThatThrownBy(() -> cliente.actualizar(datos(LocalDate.of(2015, 1, 1)), reloj))
				.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void fechaNacimientoFuturaONulaEsInvalida() {
		assertThatThrownBy(() -> Cliente.registrar(datos(LocalDate.of(2030, 1, 1)), reloj))
				.hasMessageContaining("fecha de nacimiento");
		assertThatThrownBy(() -> Cliente.registrar(datos(null), reloj))
				.hasMessageContaining("fecha de nacimiento");
	}

	@ParameterizedTest
	@ValueSource(strings = { "sin-arroba.com", "a@b", "a@b.", "a b@c.com", "a@b.c" })
	void correoConFormatoInvalido(String correo) {
		DatosCliente d = new DatosCliente(TipoIdentificacion.CC, "1", "Ana", "Gomez", correo, LocalDate.of(1990, 1, 1));
		assertThatThrownBy(() -> Cliente.registrar(d, reloj)).hasMessageContaining("correo");
	}

	@Test
	void correoNuloEsInvalido() {
		DatosCliente d = new DatosCliente(TipoIdentificacion.CC, "1", "Ana", "Gomez", null, LocalDate.of(1990, 1, 1));
		assertThatThrownBy(() -> Cliente.registrar(d, reloj)).hasMessageContaining("correo");
	}

	@Test
	void nombreOApellidoConMenosDeDosCaracteres() {
		LocalDate f = LocalDate.of(1990, 1, 1);
		assertThatThrownBy(() -> Cliente.registrar(
				new DatosCliente(TipoIdentificacion.CC, "1", " A ", "Gomez", "a@b.co", f), reloj))
				.hasMessageContaining("2 caracteres");
		assertThatThrownBy(() -> Cliente.registrar(
				new DatosCliente(TipoIdentificacion.CC, "1", "Ana", null, "a@b.co", f), reloj))
				.hasMessageContaining("2 caracteres");
	}

	@Test
	void identificacionObligatoria() {
		LocalDate f = LocalDate.of(1990, 1, 1);
		assertThatThrownBy(() -> Cliente.registrar(new DatosCliente(null, "1", "Ana", "Gomez", "a@b.co", f), reloj))
				.hasMessageContaining("identificacion");
		assertThatThrownBy(() -> Cliente.registrar(
				new DatosCliente(TipoIdentificacion.CC, "  ", "Ana", "Gomez", "a@b.co", f), reloj))
				.hasMessageContaining("identificacion");
		assertThatThrownBy(() -> Cliente.registrar(
				new DatosCliente(TipoIdentificacion.CC, null, "Ana", "Gomez", "a@b.co", f), reloj))
				.hasMessageContaining("identificacion");
	}

	@Test
	void reconstruirConservaIdYFechas() {
		LocalDateTime creada = LocalDateTime.of(2020, 1, 1, 8, 0);
		LocalDateTime modificada = LocalDateTime.of(2021, 1, 1, 8, 0);
		Cliente cliente = Cliente.reconstruir(7L, datos(LocalDate.of(1990, 1, 1)), creada, modificada);

		assertThat(cliente.getId()).isEqualTo(7L);
		assertThat(cliente.getFechaCreacion()).isEqualTo(creada);
		assertThat(cliente.getFechaModificacion()).isEqualTo(modificada);
	}
}
