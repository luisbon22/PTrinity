package com.ptrinity.application.service;

import com.ptrinity.domain.exception.RecursoNoEncontradoException;
import com.ptrinity.domain.exception.ReglaNegocioException;
import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.Cuenta;
import com.ptrinity.domain.model.EstadoCuenta;
import com.ptrinity.domain.model.EstadoDeCuenta;
import com.ptrinity.domain.model.TipoCuenta;
import com.ptrinity.domain.port.in.GestionarCuentasUseCase;
import com.ptrinity.domain.port.out.ClienteRepositoryPort;
import com.ptrinity.domain.port.out.CuentaRepositoryPort;
import com.ptrinity.domain.port.out.GeneradorNumeroCuentaPort;
import com.ptrinity.domain.port.out.MovimientoRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CuentaService implements GestionarCuentasUseCase {

	static final int INTENTOS_NUMERO_UNICO = 10;

	private final CuentaRepositoryPort cuentas;
	private final ClienteRepositoryPort clientes;
	private final MovimientoRepositoryPort movimientos;
	private final GeneradorNumeroCuentaPort generador;
	private final Clock reloj;

	public CuentaService(CuentaRepositoryPort cuentas, ClienteRepositoryPort clientes,
			MovimientoRepositoryPort movimientos, GeneradorNumeroCuentaPort generador, Clock reloj) {
		this.cuentas = cuentas;
		this.clientes = clientes;
		this.movimientos = movimientos;
		this.generador = generador;
		this.reloj = reloj;
	}

	@Override
	public Cuenta abrir(Long clienteId, TipoCuenta tipo, boolean exentaGmf) {
		buscarCliente(clienteId);
		return cuentas.guardar(Cuenta.abrir(tipo, numeroUnico(tipo), exentaGmf, clienteId, reloj));
	}

	@Override
	@Transactional(readOnly = true)
	public Cuenta obtener(Long id) {
		return cuentas.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta", id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Cuenta> listar(Long clienteId) {
		return clienteId == null ? cuentas.listar() : cuentas.listarPorCliente(clienteId);
	}

	@Override
	public Cuenta actualizarExencionGmf(Long id, boolean exentaGmf) {
		Cuenta cuenta = bloquear(id);
		cuenta.cambiarExencionGmf(exentaGmf, reloj);
		return cuentas.guardar(cuenta);
	}

	@Override
	public Cuenta cambiarEstado(Long id, EstadoCuenta estado) {
		Cuenta cuenta = bloquear(id);
		switch (estado) {
			case ACTIVA -> cuenta.activar(reloj);
			case INACTIVA -> cuenta.inactivar(reloj);
			case CANCELADA -> cuenta.cancelar(reloj);
		}
		return cuentas.guardar(cuenta);
	}

	@Override
	@Transactional(readOnly = true)
	public EstadoDeCuenta estadoDeCuenta(Long id, LocalDate desde, LocalDate hasta) {
		Cuenta cuenta = obtener(id);
		LocalDate fin = hasta != null ? hasta : LocalDate.now(reloj);
		LocalDate inicio = desde != null ? desde : fin.withDayOfMonth(1);
		if (inicio.isAfter(fin)) {
			throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la final");
		}
		var lista = movimientos.listarPorCuentaEntre(id, inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay());
		return EstadoDeCuenta.de(cuenta, buscarCliente(cuenta.getClienteId()), inicio, fin, lista);
	}

	private Cliente buscarCliente(Long clienteId) {
		return clientes.buscarPorId(clienteId).orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId));
	}

	private Cuenta bloquear(Long id) {
		return cuentas.buscarPorIdParaActualizar(id).orElseThrow(() -> new RecursoNoEncontradoException("Cuenta", id));
	}

	private String numeroUnico(TipoCuenta tipo) {
		for (int i = 0; i < INTENTOS_NUMERO_UNICO; i++) {
			String candidato = generador.generar(tipo);
			if (!cuentas.existePorNumero(candidato)) {
				return candidato;
			}
		}
		throw new IllegalStateException("No fue posible generar un numero de cuenta unico");
	}
}
