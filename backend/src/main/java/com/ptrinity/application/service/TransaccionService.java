package com.ptrinity.application.service;

import com.ptrinity.domain.exception.RecursoNoEncontradoException;
import com.ptrinity.domain.exception.ReglaNegocioException;
import com.ptrinity.domain.model.Cuenta;
import com.ptrinity.domain.model.Movimiento;
import com.ptrinity.domain.model.Naturaleza;
import com.ptrinity.domain.model.TipoTransaccion;
import com.ptrinity.domain.port.in.RealizarTransaccionesUseCase;
import com.ptrinity.domain.port.out.CuentaRepositoryPort;
import com.ptrinity.domain.port.out.MovimientoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Cada operacion corre en una sola transaccion de base de datos (ACID):
 * el saldo y su movimiento se guardan juntos o no se guarda nada, y las cuentas
 * se bloquean para que dos operaciones simultaneas no pisen el mismo saldo.
 */
@Service
@Transactional
public class TransaccionService implements RealizarTransaccionesUseCase {

	private final CuentaRepositoryPort cuentas;
	private final MovimientoRepositoryPort movimientos;
	private final Clock reloj;

	public TransaccionService(CuentaRepositoryPort cuentas, MovimientoRepositoryPort movimientos, Clock reloj) {
		this.cuentas = cuentas;
		this.movimientos = movimientos;
		this.reloj = reloj;
	}

	@Override
	public Movimiento consignar(Long cuentaId, BigDecimal monto, String descripcion) {
		Cuenta cuenta = bloquear(cuentaId);
		cuenta.acreditar(monto, reloj);
		cuenta = cuentas.guardar(cuenta);
		return movimientos.guardar(Movimiento.nuevo(cuenta, TipoTransaccion.CONSIGNACION, Naturaleza.CREDITO, monto,
				BigDecimal.ZERO.setScale(2), referencia(), null, descripcion));
	}

	@Override
	public Movimiento retirar(Long cuentaId, BigDecimal monto, String descripcion) {
		Cuenta cuenta = bloquear(cuentaId);
		BigDecimal gmf = cuenta.debitar(monto, reloj);
		cuenta = cuentas.guardar(cuenta);
		return movimientos.guardar(Movimiento.nuevo(cuenta, TipoTransaccion.RETIRO, Naturaleza.DEBITO, monto, gmf,
				referencia(), null, descripcion));
	}

	@Override
	public List<Movimiento> transferir(Long cuentaOrigenId, Long cuentaDestinoId, BigDecimal monto, String descripcion) {
		if (cuentaOrigenId.equals(cuentaDestinoId)) {
			throw new ReglaNegocioException("La cuenta de origen y la de destino deben ser diferentes");
		}
		// Bloqueo siempre en el mismo orden (id ascendente) para evitar interbloqueos
		Cuenta primera = bloquear(Math.min(cuentaOrigenId, cuentaDestinoId));
		Cuenta segunda = bloquear(Math.max(cuentaOrigenId, cuentaDestinoId));
		Cuenta origen = primera.getId().equals(cuentaOrigenId) ? primera : segunda;
		Cuenta destino = origen == primera ? segunda : primera;

		BigDecimal gmf = origen.debitar(monto, reloj);
		destino.acreditar(monto, reloj);
		origen = cuentas.guardar(origen);
		destino = cuentas.guardar(destino);

		String referencia = referencia();
		Movimiento debito = movimientos.guardar(Movimiento.nuevo(origen, TipoTransaccion.TRANSFERENCIA,
				Naturaleza.DEBITO, monto, gmf, referencia, destino.getNumero(), descripcion));
		Movimiento credito = movimientos.guardar(Movimiento.nuevo(destino, TipoTransaccion.TRANSFERENCIA,
				Naturaleza.CREDITO, monto, BigDecimal.ZERO.setScale(2), referencia, origen.getNumero(), descripcion));
		return List.of(debito, credito);
	}

	@Override
	@Transactional(readOnly = true)
	public Movimiento obtener(Long id) {
		return movimientos.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Movimiento", id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Movimiento> listarPorCuenta(Long cuentaId) {
		cuentas.buscarPorId(cuentaId).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta", cuentaId));
		return movimientos.listarPorCuenta(cuentaId);
	}

	private Cuenta bloquear(Long id) {
		return cuentas.buscarPorIdParaActualizar(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta", id));
	}

	private static String referencia() {
		return UUID.randomUUID().toString();
	}
}
