package com.ptrinity.bank.domain.exception;

/**
 * Excepción base del dominio. No depende de ningún framework.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
