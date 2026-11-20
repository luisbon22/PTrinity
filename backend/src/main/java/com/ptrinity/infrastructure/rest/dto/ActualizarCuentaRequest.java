package com.ptrinity.infrastructure.rest.dto;

import jakarta.validation.constraints.NotNull;

public record ActualizarCuentaRequest(@NotNull Boolean exentaGmf) {
}
