package com.ptrinity.infrastructure.persistence.entity;

import com.ptrinity.domain.model.EstadoCuenta;
import com.ptrinity.domain.model.TipoCuenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuentas")
public class CuentaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_cuenta", nullable = false, length = 20, updatable = false)
	private TipoCuenta tipo;

	@Column(name = "numero_cuenta", nullable = false, length = 10, unique = true, updatable = false)
	private String numero;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoCuenta estado;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal saldo;

	@Column(name = "exenta_gmf", nullable = false)
	private boolean exentaGmf;

	@Column(name = "cliente_id", nullable = false, updatable = false)
	private Long clienteId;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	@Column(name = "fecha_modificacion", nullable = false)
	private LocalDateTime fechaModificacion;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public TipoCuenta getTipo() {
		return tipo;
	}

	public void setTipo(TipoCuenta tipo) {
		this.tipo = tipo;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public EstadoCuenta getEstado() {
		return estado;
	}

	public void setEstado(EstadoCuenta estado) {
		this.estado = estado;
	}

	public BigDecimal getSaldo() {
		return saldo;
	}

	public void setSaldo(BigDecimal saldo) {
		this.saldo = saldo;
	}

	public boolean isExentaGmf() {
		return exentaGmf;
	}

	public void setExentaGmf(boolean exentaGmf) {
		this.exentaGmf = exentaGmf;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public void setClienteId(Long clienteId) {
		this.clienteId = clienteId;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public LocalDateTime getFechaModificacion() {
		return fechaModificacion;
	}

	public void setFechaModificacion(LocalDateTime fechaModificacion) {
		this.fechaModificacion = fechaModificacion;
	}
}
