package com.ptrinity.infrastructure.rest.dto;

import com.ptrinity.domain.model.DatosCliente;
import com.ptrinity.domain.model.TipoIdentificacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Datos para registrar o actualizar un cliente")
public record ClienteRequest(
		@NotNull @Schema(example = "CC") TipoIdentificacion tipoIdentificacion,
		@NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9]+", message = "solo admite letras y numeros")
		@Schema(example = "1020304050") String numeroIdentificacion,
		@NotBlank @Size(min = 2, max = 100) @Schema(example = "Ana Maria") String nombres,
		@NotBlank @Size(min = 2, max = 100) @Schema(example = "Gomez") String apellido,
		@NotBlank @Size(max = 150)
		@Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$", message = "debe tener el formato xxxx@xxxxx.xxx")
		@Schema(example = "ana.gomez@correo.com") String correo,
		@NotNull @Past @Schema(example = "1990-05-14") LocalDate fechaNacimiento) {

	public DatosCliente aDominio() {
		return new DatosCliente(tipoIdentificacion, numeroIdentificacion, nombres, apellido, correo, fechaNacimiento);
	}
}
