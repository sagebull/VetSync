package com.vetsync.application.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Correo electronico o contrasena incorrectos");
    }
}
