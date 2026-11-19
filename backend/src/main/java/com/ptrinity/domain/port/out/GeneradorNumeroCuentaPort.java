package com.ptrinity.domain.port.out;

import com.ptrinity.domain.model.TipoCuenta;

/** Estrategia para generar candidatos de numero de cuenta (la unicidad la verifica el servicio). */
public interface GeneradorNumeroCuentaPort {

	String generar(TipoCuenta tipo);
}
