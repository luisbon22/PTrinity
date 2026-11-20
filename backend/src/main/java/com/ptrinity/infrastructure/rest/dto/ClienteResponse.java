package com.ptrinity.infrastructure.rest.dto;

import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.TipoIdentificacion;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteResponse(
		Long id,
		TipoIdentificacion tipoIdentificacion,
		String numeroIdentificacion,
		String nombres,
		String apellido,
		String correo,
		LocalDate fechaNacimiento,
		LocalDateTime fechaCreacion,
		LocalDateTime fechaModificacion) {

	public static ClienteResponse de(Cliente c) {
		return new ClienteResponse(c.getId(), c.getTipoIdentificacion(), c.getNumeroIdentificacion(), c.getNombres(),
				c.getApellido(), c.getCorreo(), c.getFechaNacimiento(), c.getFechaCreacion(), c.getFechaModificacion());
	}
}
