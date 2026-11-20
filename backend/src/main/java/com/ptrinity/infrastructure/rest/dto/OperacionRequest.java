package com.ptrinity.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Consignacion o retiro sobre una cuenta")
public record OperacionRequest(
		@NotNull @Schema(example = "1") Long cuentaId,
		@NotNull @Positive @Digits(integer = 16, fraction = 2) @Schema(example = "150000.00") BigDecimal monto,
		@Size(max = 255) @Schema(example = "Consignacion en ventanilla") String descripcion) {
}
