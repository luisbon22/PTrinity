package com.ptrinity.infrastructure.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Transferencia entre dos cuentas existentes")
public record TransferenciaRequest(
		@NotNull @Schema(example = "1") Long cuentaOrigenId,
		@NotNull @Schema(example = "2") Long cuentaDestinoId,
		@NotNull @Positive @Digits(integer = 16, fraction = 2) @Schema(example = "50000.00") BigDecimal monto,
		@Size(max = 255) @Schema(example = "Pago arriendo") String descripcion) {
}
