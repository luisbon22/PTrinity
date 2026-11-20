package com.ptrinity.domain.model;

import java.time.LocalDate;

/** Datos editables de un cliente (comando de creacion y actualizacion). */
public record DatosCliente(
		TipoIdentificacion tipoIdentificacion,
		String numeroIdentificacion,
		String nombres,
		String apellido,
		String correo,
		LocalDate fechaNacimiento) {
}
