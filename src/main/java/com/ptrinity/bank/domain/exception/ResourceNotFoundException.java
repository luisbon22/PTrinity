package com.ptrinity.bank.domain.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " no encontrado: " + identifier);
    }
}
