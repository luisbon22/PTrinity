package com.ptrinity.domain.exception;

/** Una operacion viola una regla del negocio financiero. */
public class ReglaNegocioException extends RuntimeException {

	public ReglaNegocioException(String mensaje) {
		super(mensaje);
	}
}
