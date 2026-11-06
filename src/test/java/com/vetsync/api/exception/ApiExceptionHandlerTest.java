package com.vetsync.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsync.application.exception.InvalidCredentialsException;
import com.vetsync.application.exception.ResourceConflictException;
import com.vetsync.application.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void notFoundMapsTo404WithExceptionMessageAsDetail() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Mascota", 9L);

        ProblemDetail body = handler.handleNotFound(exception);

        assertThat(body.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.getDetail()).isEqualTo(exception.getMessage());
    }

    @Test
    void invalidCredentialsMapsTo401() {
        InvalidCredentialsException exception = new InvalidCredentialsException();

        ProblemDetail body = handler.handleInvalidCredentials(exception);

        assertThat(body.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(body.getDetail()).isEqualTo(exception.getMessage());
    }

    @Test
    void resourceConflictKeepsItsOwnMessage() {
        ResourceConflictException exception = new ResourceConflictException("La cita se solapa");

        ProblemDetail body = handler.handleConflict(exception);

        assertThat(body.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(body.getDetail()).isEqualTo("La cita se solapa");
    }

    @Test
    void dataIntegrityViolationMapsTo409WithGenericMessage() {
        ProblemDetail body = handler.handleConflict(new DataIntegrityViolationException("duplicate key"));

        assertThat(body.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(body.getDetail()).isEqualTo("La operacion viola una restriccion de integridad de datos");
    }

    @Test
    void concurrencyFailureMapsTo409WithGenericMessage() {
        ProblemDetail body = handler.handleConflict(new ConcurrencyFailureException("optimistic lock"));

        assertThat(body.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(body.getDetail()).isEqualTo("La operacion viola una restriccion de integridad de datos");
    }

    @Test
    void methodArgumentNotValidListsEachFieldErrorWithItsMessage() throws NoSuchMethodException {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new org.springframework.validation.FieldError("request", "email", "formato invalido"));
        binding.addError(new org.springframework.validation.FieldError("request", "phone", null, false,
                null, null, null));
        MethodParameter parameter = new MethodParameter(
                ApiExceptionHandlerTest.class.getDeclaredMethod("methodArgumentNotValidListsEachFieldErrorWithItsMessage"), -1);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, binding);

        ProblemDetail body = handler.handleValidation(exception);

        assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.getDetail()).isEqualTo("La solicitud contiene datos no validos");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> errors = (List<Map<String, String>>) body.getProperties().get("errors");
        assertThat(errors).containsExactly(
                Map.of("field", "email", "message", "formato invalido"),
                Map.of("field", "phone", "message", "Valor no valido"));
    }

    @Test
    void unreadableBodyMapsTo400() {
        ProblemDetail body = handler.handleUnreadableRequest();

        assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.getDetail())
                .isEqualTo("El cuerpo de la solicitud contiene un valor o formato no valido");
    }

    @Test
    void constraintViolationMapsTo400() {
        ProblemDetail body = handler.handleMethodValidation();

        assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.getDetail()).isEqualTo("Uno o mas parametros de la solicitud no son validos");
    }

    @Test
    void illegalArgumentMapsTo400WithExceptionMessage() {
        ProblemDetail body = handler.handleInvalidArgument(new IllegalArgumentException("mes fuera de rango"));

        assertThat(body.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.getDetail()).isEqualTo("mes fuera de rango");
    }
}
