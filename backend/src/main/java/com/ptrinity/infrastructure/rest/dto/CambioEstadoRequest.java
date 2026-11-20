package com.ptrinity.infrastructure.rest.dto;

import com.ptrinity.domain.model.EstadoCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(@NotNull @Schema(example = "INACTIVA") EstadoCuenta estado) {
}
