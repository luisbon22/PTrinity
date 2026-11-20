package com.ptrinity.infrastructure.persistence.entity;

import com.ptrinity.domain.model.Naturaleza;
import com.ptrinity.domain.model.TipoTransaccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Los movimientos son un libro contable: se insertan y nunca se modifican. */
@Entity
@Immutable
@Table(name = "movimientos")
public class MovimientoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "cuenta_id", nullable = false)
	private Long cuentaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoTransaccion tipo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Naturaleza naturaleza;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal monto;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal gmf;

	@Column(name = "saldo_resultante", nullable = false, precision = 18, scale = 2)
	private BigDecimal saldoResultante;

	@Column(nullable = false, length = 36)
	private String referencia;

	@Column(name = "cuenta_contraparte", length = 10)
	private String cuentaContraparte;

	@Column(length = 255)
	private String descripcion;

	@Column(nullable = false)
	private LocalDateTime fecha;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCuentaId() {
		return cuentaId;
	}

	public void setCuentaId(Long cuentaId) {
		this.cuentaId = cuentaId;
	}

	public TipoTransaccion getTipo() {
		return tipo;
	}

	public void setTipo(TipoTransaccion tipo) {
		this.tipo = tipo;
	}

	public Naturaleza getNaturaleza() {
		return naturaleza;
	}

	public void setNaturaleza(Naturaleza naturaleza) {
		this.naturaleza = naturaleza;
	}

	public BigDecimal getMonto() {
		return monto;
	}

	public void setMonto(BigDecimal monto) {
		this.monto = monto;
	}

	public BigDecimal getGmf() {
		return gmf;
	}

	public void setGmf(BigDecimal gmf) {
		this.gmf = gmf;
	}

	public BigDecimal getSaldoResultante() {
		return saldoResultante;
	}

	public void setSaldoResultante(BigDecimal saldoResultante) {
		this.saldoResultante = saldoResultante;
	}

	public String getReferencia() {
		return referencia;
	}

	public void setReferencia(String referencia) {
		this.referencia = referencia;
	}

	public String getCuentaContraparte() {
		return cuentaContraparte;
	}

	public void setCuentaContraparte(String cuentaContraparte) {
		this.cuentaContraparte = cuentaContraparte;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
}
