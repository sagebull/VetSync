package com.vetsync.application.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " no encontrado con identificador " + id);
    }
}
