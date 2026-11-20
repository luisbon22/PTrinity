package com.ptrinity.infrastructure.rest.dto;

import com.ptrinity.domain.model.TipoCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Apertura de una cuenta de ahorros o corriente; nace ACTIVA y con saldo 0")
public record AperturaCuentaRequest(
		@NotNull @Schema(example = "1") Long clienteId,
		@NotNull @Schema(example = "AHORROS") TipoCuenta tipoCuenta,
		@Schema(example = "false", defaultValue = "false") Boolean exentaGmf) {

	public boolean esExenta() {
		return Boolean.TRUE.equals(exentaGmf);
	}
}
