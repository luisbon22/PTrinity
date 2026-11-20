package com.ptrinity.infrastructure.persistence.adapter;

import com.ptrinity.domain.model.TipoCuenta;
import com.ptrinity.domain.port.out.GeneradorNumeroCuentaPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Prefijo del tipo de cuenta + 8 digitos aleatorios = 10 digitos. */
@Component
class GeneradorNumeroCuentaAleatorio implements GeneradorNumeroCuentaPort {

	private final SecureRandom aleatorio = new SecureRandom();

	@Override
	public String generar(TipoCuenta tipo) {
		return tipo.prefijo() + String.format("%08d", aleatorio.nextInt(100_000_000));
	}
}
