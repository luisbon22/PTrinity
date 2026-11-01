package com.ptrinity.bank.domain.exception;

/**
 * Se lanza cuando una operación viola una regla de negocio
 * (cliente menor de edad, saldo insuficiente, cuenta inactiva, etc.).
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
