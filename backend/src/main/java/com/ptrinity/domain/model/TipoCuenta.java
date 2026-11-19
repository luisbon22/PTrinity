package com.ptrinity.domain.model;

/** Cada tipo de cuenta conoce el prefijo de su numero (regla de negocio). */
public enum TipoCuenta {
	AHORROS("53"),
	CORRIENTE("33");

	private final String prefijo;

	TipoCuenta(String prefijo) {
		this.prefijo = prefijo;
	}

	public String prefijo() {
		return prefijo;
	}
}
