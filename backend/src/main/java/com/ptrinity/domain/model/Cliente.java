package com.ptrinity.domain.model;

import com.ptrinity.domain.exception.ReglaNegocioException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.regex.Pattern;

/**
 * Cliente de la entidad financiera. Las reglas del negocio viven aqui y no en
 * los controladores ni en la base de datos, para que cualquier adaptador las respete.
 */
public class Cliente {

	public static final int EDAD_MINIMA = 18;
	public static final int LONGITUD_MINIMA_NOMBRE = 2;
	private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$");

	private Long id;
	private TipoIdentificacion tipoIdentificacion;
	private String numeroIdentificacion;
	private String nombres;
	private String apellido;
	private String correo;
	private LocalDate fechaNacimiento;
	private LocalDateTime fechaCreacion;
	private LocalDateTime fechaModificacion;

	private Cliente() {
	}

	public static Cliente registrar(DatosCliente datos, Clock reloj) {
		Cliente cliente = new Cliente();
		cliente.aplicar(datos, reloj);
		cliente.fechaCreacion = cliente.fechaModificacion;
		return cliente;
	}

	/** Reconstruye un cliente ya persistido, sin volver a validar ni tocar fechas. */
	public static Cliente reconstruir(Long id, DatosCliente datos, LocalDateTime fechaCreacion,
			LocalDateTime fechaModificacion) {
		Cliente cliente = new Cliente();
		cliente.id = id;
		cliente.copiar(datos);
		cliente.fechaCreacion = fechaCreacion;
		cliente.fechaModificacion = fechaModificacion;
		return cliente;
	}

	public void actualizar(DatosCliente datos, Clock reloj) {
		aplicar(datos, reloj);
	}

	private void aplicar(DatosCliente datos, Clock reloj) {
		validar(datos, LocalDate.now(reloj));
		copiar(datos);
		fechaModificacion = LocalDateTime.now(reloj);
	}

	private void copiar(DatosCliente datos) {
		tipoIdentificacion = datos.tipoIdentificacion();
		numeroIdentificacion = datos.numeroIdentificacion().trim();
		nombres = datos.nombres().trim();
		apellido = datos.apellido().trim();
		correo = datos.correo().trim().toLowerCase();
		fechaNacimiento = datos.fechaNacimiento();
	}

	private static void validar(DatosCliente datos, LocalDate hoy) {
		if (datos.tipoIdentificacion() == null || estaVacio(datos.numeroIdentificacion())) {
			throw new ReglaNegocioException("El tipo y el numero de identificacion son obligatorios");
		}
		if (largo(datos.nombres()) < LONGITUD_MINIMA_NOMBRE || largo(datos.apellido()) < LONGITUD_MINIMA_NOMBRE) {
			throw new ReglaNegocioException("Los nombres y el apellido deben tener al menos 2 caracteres");
		}
		if (datos.correo() == null || !CORREO.matcher(datos.correo().trim()).matches()) {
			throw new ReglaNegocioException("El correo electronico no tiene un formato valido");
		}
		if (datos.fechaNacimiento() == null || datos.fechaNacimiento().isAfter(hoy)) {
			throw new ReglaNegocioException("La fecha de nacimiento es obligatoria y no puede ser futura");
		}
		if (Period.between(datos.fechaNacimiento(), hoy).getYears() < EDAD_MINIMA) {
			throw new ReglaNegocioException("El cliente debe ser mayor de edad");
		}
	}

	private static boolean estaVacio(String valor) {
		return valor == null || valor.isBlank();
	}

	private static int largo(String valor) {
		return valor == null ? 0 : valor.trim().length();
	}

	public DatosCliente datos() {
		return new DatosCliente(tipoIdentificacion, numeroIdentificacion, nombres, apellido, correo, fechaNacimiento);
	}

	public Long getId() {
		return id;
	}

	public TipoIdentificacion getTipoIdentificacion() {
		return tipoIdentificacion;
	}

	public String getNumeroIdentificacion() {
		return numeroIdentificacion;
	}

	public String getNombres() {
		return nombres;
	}

	public String getApellido() {
		return apellido;
	}

	public String getCorreo() {
		return correo;
	}

	public LocalDate getFechaNacimiento() {
		return fechaNacimiento;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public LocalDateTime getFechaModificacion() {
		return fechaModificacion;
	}
}
