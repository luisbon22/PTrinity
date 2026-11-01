package com.ptrinity.bank.infrastructure.adapter.in.web.exception;

import com.ptrinity.bank.domain.exception.BusinessRuleException;
import com.ptrinity.bank.domain.exception.DuplicateResourceException;
import com.ptrinity.bank.domain.exception.ResourceNotFoundException;
import com.ptrinity.bank.infrastructure.adapter.in.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Traduce las excepciones del dominio y de la infraestructura a respuestas HTTP homogéneas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", request, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleUnreadable(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo o los parámetros de la solicitud no son válidos",
                request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex,
                                                         HttpServletRequest request) {
        log.warn("Violación de integridad de datos", ex);
        return build(HttpStatus.CONFLICT, "La operación viola una restricción de integridad de los datos",
                request, List.of());
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrency(ConcurrencyFailureException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "El recurso fue modificado por otra operación, intente nuevamente",
                request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", request, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                List<ErrorResponse.FieldError> fields) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
