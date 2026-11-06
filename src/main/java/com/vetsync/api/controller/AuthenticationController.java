package com.vetsync.api.controller;

import com.vetsync.api.dto.AuthenticationResponse;
import com.vetsync.api.dto.LoginRequest;
import com.vetsync.api.dto.OwnerRegistrationRequest;
import com.vetsync.api.dto.RegistrationResponse;
import com.vetsync.application.service.AuthenticationService;
import com.vetsync.application.service.OwnerRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authentication;
    private final OwnerRegistrationService registrations;

    public AuthenticationController(AuthenticationService authentication, OwnerRegistrationService registrations) {
        this.authentication = authentication;
        this.registrations = registrations;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("permitAll()")
    public RegistrationResponse register(@Valid @RequestBody OwnerRegistrationRequest request) {
        var registration = registrations.submit(
                request.fullName(), request.phone(), request.email(), request.address(), request.password());
        return new RegistrationResponse(registration.getId(), "PENDING",
                "La solicitud requiere verificacion de identidad por personal autorizado antes de activar la cuenta");
    }

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public AuthenticationResponse login(@Valid @RequestBody LoginRequest request) {
        return toResponse(authentication.login(request.email(), request.password()));
    }

    private AuthenticationResponse toResponse(AuthenticationService.AuthenticationToken token) {
        return new AuthenticationResponse(token.accessToken(), "Bearer", token.expiresAt());
    }
}
