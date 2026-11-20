package com.ptrinity.infrastructure.rest.error;

import com.ptrinity.domain.exception.RecursoDuplicadoException;
import com.ptrinity.domain.exception.RecursoNoEncontradoException;
import com.ptrinity.domain.exception.ReglaNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce las excepciones a respuestas RFC 9457 (application/problem+json). */
@RestControllerAdvice
public class ManejadorErrores {

	private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
	}

	@ExceptionHandler(RecursoDuplicadoException.class)
	ProblemDetail duplicado(RecursoDuplicadoException ex) {
		return problema(HttpStatus.CONFLICT, "Recurso duplicado", ex.getMessage());
	}

	@ExceptionHandler(ReglaNegocioException.class)
	ProblemDetail reglaNegocio(ReglaNegocioException ex) {
		return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio", ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail validacion(MethodArgumentNotValidException ex) {
		Map<String, String> errores = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
		ProblemDetail p = problema(HttpStatus.BAD_REQUEST, "Datos invalidos", "Revise los campos marcados");
		p.setProperty("errores", errores);
		return p;
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class })
	ProblemDetail solicitudMalFormada(Exception ex) {
		return problema(HttpStatus.BAD_REQUEST, "Solicitud mal formada",
				"El cuerpo o los parametros no tienen el formato esperado");
	}

	/** Ultima barrera: una restriccion de la base de datos detuvo un dato invalido. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail integridad(DataIntegrityViolationException ex) {
		log.warn("Violacion de integridad", ex);
		return problema(HttpStatus.CONFLICT, "Conflicto de integridad",
				"La operacion viola una restriccion de integridad de los datos");
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
		ProblemDetail p = ProblemDetail.forStatusAndDetail(estado, detalle);
		p.setTitle(titulo);
		return p;
	}
}
