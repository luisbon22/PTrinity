package com.ptrinity.domain.model;

import com.ptrinity.domain.exception.ReglaNegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Producto financiero (cuenta de ahorros o corriente).
 * <ul>
 * <li>Ahorros: el saldo nunca puede ser negativo.</li>
 * <li>Corriente: admite sobregiro hasta {@link #CUPO_SOBREGIRO_CORRIENTE}.</li>
 * <li>Cuentas no exentas pagan GMF (4 x 1000) sobre cada debito.</li>
 * </ul>
 */
public class Cuenta {

	public static final BigDecimal CUPO_SOBREGIRO_CORRIENTE = new BigDecimal("1000000.00");
	public static final BigDecimal TARIFA_GMF = new BigDecimal("0.004");

	private Long id;
	private TipoCuenta tipo;
	private String numero;
	private EstadoCuenta estado;
	private BigDecimal saldo;
	private boolean exentaGmf;
	private Long clienteId;
	private LocalDateTime fechaCreacion;
	private LocalDateTime fechaModificacion;

	private Cuenta() {
	}

	/** Toda cuenta nueva nace activa y con saldo cero. */
	public static Cuenta abrir(TipoCuenta tipo, String numero, boolean exentaGmf, Long clienteId, Clock reloj) {
		if (tipo == null) {
			throw new ReglaNegocioException("El tipo de cuenta es obligatorio");
		}
		if (clienteId == null) {
			throw new ReglaNegocioException("Una cuenta solo puede existir vinculada a un cliente");
		}
		if (numero == null || !numero.matches("\\d{10}") || !numero.startsWith(tipo.prefijo())) {
			throw new ReglaNegocioException("El numero de cuenta debe tener 10 digitos e iniciar en " + tipo.prefijo());
		}
		Cuenta cuenta = new Cuenta();
		cuenta.tipo = tipo;
		cuenta.numero = numero;
		cuenta.estado = EstadoCuenta.ACTIVA;
		cuenta.saldo = BigDecimal.ZERO.setScale(2);
		cuenta.exentaGmf = exentaGmf;
		cuenta.clienteId = clienteId;
		cuenta.fechaCreacion = LocalDateTime.now(reloj);
		cuenta.fechaModificacion = cuenta.fechaCreacion;
		return cuenta;
	}

	public static Cuenta reconstruir(Long id, TipoCuenta tipo, String numero, EstadoCuenta estado, BigDecimal saldo,
			boolean exentaGmf, Long clienteId, LocalDateTime fechaCreacion, LocalDateTime fechaModificacion) {
		Cuenta cuenta = new Cuenta();
		cuenta.id = id;
		cuenta.tipo = tipo;
		cuenta.numero = numero;
		cuenta.estado = estado;
		cuenta.saldo = saldo;
		cuenta.exentaGmf = exentaGmf;
		cuenta.clienteId = clienteId;
		cuenta.fechaCreacion = fechaCreacion;
		cuenta.fechaModificacion = fechaModificacion;
		return cuenta;
	}

	public void activar(Clock reloj) {
		validarNoCancelada();
		cambiarEstado(EstadoCuenta.ACTIVA, reloj);
	}

	public void inactivar(Clock reloj) {
		validarNoCancelada();
		cambiarEstado(EstadoCuenta.INACTIVA, reloj);
	}

	public void cancelar(Clock reloj) {
		validarNoCancelada();
		if (saldo.signum() != 0) {
			throw new ReglaNegocioException("Solo se puede cancelar una cuenta con saldo igual a $0");
		}
		cambiarEstado(EstadoCuenta.CANCELADA, reloj);
	}

	public void cambiarExencionGmf(boolean exenta, Clock reloj) {
		validarNoCancelada();
		exentaGmf = exenta;
		fechaModificacion = LocalDateTime.now(reloj);
	}

	public void acreditar(BigDecimal monto, Clock reloj) {
		BigDecimal valor = validarMovimiento(monto);
		saldo = saldo.add(valor);
		fechaModificacion = LocalDateTime.now(reloj);
	}

	/** Debita el monto mas el GMF que corresponda y devuelve el GMF cobrado. */
	public BigDecimal debitar(BigDecimal monto, Clock reloj) {
		BigDecimal valor = validarMovimiento(monto);
		BigDecimal gmf = calcularGmf(valor);
		BigDecimal total = valor.add(gmf);
		if (total.compareTo(saldoDisponible()) > 0) {
			throw new ReglaNegocioException("Saldo insuficiente: disponible " + saldoDisponible() + ", requerido " + total);
		}
		saldo = saldo.subtract(total);
		fechaModificacion = LocalDateTime.now(reloj);
		return gmf;
	}

	public BigDecimal calcularGmf(BigDecimal monto) {
		if (exentaGmf) {
			return BigDecimal.ZERO.setScale(2);
		}
		return monto.multiply(TARIFA_GMF).setScale(2, RoundingMode.HALF_UP);
	}

	/** Lo que el cliente puede usar: el saldo mas el cupo de sobregiro (solo corriente). */
	public BigDecimal saldoDisponible() {
		return tipo == TipoCuenta.CORRIENTE ? saldo.add(CUPO_SOBREGIRO_CORRIENTE) : saldo;
	}

	private BigDecimal validarMovimiento(BigDecimal monto) {
		if (estado != EstadoCuenta.ACTIVA) {
			throw new ReglaNegocioException("La cuenta " + numero + " no esta activa");
		}
		if (monto == null || monto.signum() <= 0) {
			throw new ReglaNegocioException("El monto debe ser mayor a cero");
		}
		if (monto.stripTrailingZeros().scale() > 2) {
			throw new ReglaNegocioException("El monto admite maximo dos decimales");
		}
		return monto.setScale(2);
	}

	private void validarNoCancelada() {
		if (estado == EstadoCuenta.CANCELADA) {
			throw new ReglaNegocioException("La cuenta " + numero + " esta cancelada");
		}
	}

	private void cambiarEstado(EstadoCuenta nuevo, Clock reloj) {
		estado = nuevo;
		fechaModificacion = LocalDateTime.now(reloj);
	}

	public Long getId() {
		return id;
	}

	public TipoCuenta getTipo() {
		return tipo;
	}

	public String getNumero() {
		return numero;
	}

	public EstadoCuenta getEstado() {
		return estado;
	}

	public BigDecimal getSaldo() {
		return saldo;
	}

	public boolean isExentaGmf() {
		return exentaGmf;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public LocalDateTime getFechaModificacion() {
		return fechaModificacion;
	}
}
