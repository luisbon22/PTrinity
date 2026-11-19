package com.ptrinity.infrastructure.persistence.adapter;

import com.ptrinity.domain.model.Cliente;
import com.ptrinity.domain.model.Cuenta;
import com.ptrinity.domain.model.DatosCliente;
import com.ptrinity.domain.model.Movimiento;
import com.ptrinity.infrastructure.persistence.entity.ClienteEntity;
import com.ptrinity.infrastructure.persistence.entity.CuentaEntity;
import com.ptrinity.infrastructure.persistence.entity.MovimientoEntity;

/** Traduce entre el modelo de dominio y las entidades JPA (patron Mapper). */
final class PersistenciaMapper {

	private PersistenciaMapper() {
	}

	static ClienteEntity aEntidad(Cliente c) {
		ClienteEntity e = new ClienteEntity();
		e.setId(c.getId());
		e.setTipoIdentificacion(c.getTipoIdentificacion());
		e.setNumeroIdentificacion(c.getNumeroIdentificacion());
		e.setNombres(c.getNombres());
		e.setApellido(c.getApellido());
		e.setCorreo(c.getCorreo());
		e.setFechaNacimiento(c.getFechaNacimiento());
		e.setFechaCreacion(c.getFechaCreacion());
		e.setFechaModificacion(c.getFechaModificacion());
		return e;
	}

	static Cliente aDominio(ClienteEntity e) {
		DatosCliente datos = new DatosCliente(e.getTipoIdentificacion(), e.getNumeroIdentificacion(), e.getNombres(),
				e.getApellido(), e.getCorreo(), e.getFechaNacimiento());
		return Cliente.reconstruir(e.getId(), datos, e.getFechaCreacion(), e.getFechaModificacion());
	}

	static CuentaEntity aEntidad(Cuenta c) {
		CuentaEntity e = new CuentaEntity();
		e.setId(c.getId());
		e.setTipo(c.getTipo());
		e.setNumero(c.getNumero());
		e.setEstado(c.getEstado());
		e.setSaldo(c.getSaldo());
		e.setExentaGmf(c.isExentaGmf());
		e.setClienteId(c.getClienteId());
		e.setFechaCreacion(c.getFechaCreacion());
		e.setFechaModificacion(c.getFechaModificacion());
		return e;
	}

	static Cuenta aDominio(CuentaEntity e) {
		return Cuenta.reconstruir(e.getId(), e.getTipo(), e.getNumero(), e.getEstado(), e.getSaldo(), e.isExentaGmf(),
				e.getClienteId(), e.getFechaCreacion(), e.getFechaModificacion());
	}

	static MovimientoEntity aEntidad(Movimiento m) {
		MovimientoEntity e = new MovimientoEntity();
		e.setId(m.id());
		e.setCuentaId(m.cuentaId());
		e.setTipo(m.tipo());
		e.setNaturaleza(m.naturaleza());
		e.setMonto(m.monto());
		e.setGmf(m.gmf());
		e.setSaldoResultante(m.saldoResultante());
		e.setReferencia(m.referencia());
		e.setCuentaContraparte(m.cuentaContraparte());
		e.setDescripcion(m.descripcion());
		e.setFecha(m.fecha());
		return e;
	}

	static Movimiento aDominio(MovimientoEntity e) {
		return new Movimiento(e.getId(), e.getCuentaId(), e.getTipo(), e.getNaturaleza(), e.getMonto(), e.getGmf(),
				e.getSaldoResultante(), e.getReferencia(), e.getCuentaContraparte(), e.getDescripcion(), e.getFecha());
	}
}
