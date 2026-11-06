package com.vetsync.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.api.exception.ApiExceptionHandler;
import com.vetsync.application.exception.InvalidCredentialsException;
import com.vetsync.application.service.AuthenticationService;
import com.vetsync.application.service.AuthenticationService.AuthenticationToken;
import com.vetsync.application.service.OwnerRegistrationService;
import com.vetsync.domain.model.OwnerRegistration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthenticationControllerTest {

    private final AuthenticationService authentication = mock(AuthenticationService.class);
    private final OwnerRegistrationService registrations = mock(OwnerRegistrationService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthenticationController(authentication, registrations))
            .setControllerAdvice(new ApiExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json().build()))
            .build();

    @Test
    void registerSubmitsRequestAndReturnsPendingRegistration() throws Exception {
        OwnerRegistration request = mock(OwnerRegistration.class);
        when(request.getId()).thenReturn(8L);
        when(registrations.submit("Luis Gomez", "555-0200", "luis@example.com", "Avenida 2",
                "contrasena-segura-1")).thenReturn(request);

        mockMvc.perform(post("/api/auth/register").contentType("application/json").content("""
                        {"fullName":"Luis Gomez","phone":"555-0200","email":"luis@example.com",
                         "address":"Avenida 2","password":"contrasena-segura-1"}
                        """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value(8))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType("application/json").content("""
                        {"fullName":"Luis","phone":"1","email":"luis@example.com","address":"x","password":"corta"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsBearerToken() throws Exception {
        Instant expiresAt = Instant.parse("2030-01-01T00:15:00Z");
        when(authentication.login("ana@example.com", "clave-de-prueba")).thenReturn(
                new AuthenticationToken("jwt-value", expiresAt));

        mockMvc.perform(post("/api/auth/login").contentType("application/json").content("""
                        {"email":"ana@example.com","password":"clave-de-prueba"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void loginWithBadCredentialsReturnsUnauthorized() throws Exception {
        when(authentication.login(eq("ana@example.com"), any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login").contentType("application/json").content("""
                        {"email":"ana@example.com","password":"clave-incorrecta"}
                        """))
                .andExpect(status().isUnauthorized());
    }
}
