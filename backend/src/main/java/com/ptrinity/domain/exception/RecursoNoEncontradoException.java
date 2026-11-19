package com.ptrinity.domain.exception;

public class RecursoNoEncontradoException extends RuntimeException {

	public RecursoNoEncontradoException(String recurso, Object id) {
		super(recurso + " con id " + id + " no existe");
	}
}
