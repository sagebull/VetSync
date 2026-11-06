package com.vetsync.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.application.exception.InvalidCredentialsException;
import com.vetsync.domain.model.AccountRole;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.UserAccount;
import com.vetsync.domain.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock
    private UserAccountRepository accounts;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-05T16:00:00Z"), ZoneOffset.UTC);
        service = new AuthenticationService(accounts, passwordEncoder, jwtEncoder, clock);
    }

    @Test
    void loginNormalizesEmailAndIssuesTokenForCorrectPassword() {
        Owner owner = mock(Owner.class);
        when(owner.getId()).thenReturn(42L);
        UserAccount account = new UserAccount("ana@example.com", "{pbkdf2}stored-hash", AccountRole.OWNER, owner);
        when(accounts.findByEmail("ana@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secure-long-password", "{pbkdf2}stored-hash")).thenReturn(true);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(Jwt.withTokenValue("signed-test-token").header("alg", "HS256")
                        .claim("sub", "ana@example.com").build());

        AuthenticationService.AuthenticationToken result = service.login(
                "ANA@example.com ", "secure-long-password");

        assertEquals("signed-test-token", result.accessToken());
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void loginRejectsUnknownEmailWithoutDisclosingWhetherItExists() {
        when(accounts.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> service.login("missing@example.com", "wrong-password"));

        assertEquals("Correo electronico o contrasena incorrectos", exception.getMessage());
    }

    @Test
    void loginRejectsIncorrectPassword() {
        UserAccount account = new UserAccount("ana@example.com", "{pbkdf2}stored-hash", AccountRole.OWNER,
                new Owner("Ana Lopez", "5551234567", "ana@example.com", "Calle 1"));
        when(accounts.findByEmail("ana@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", "{pbkdf2}stored-hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> service.login("ana@example.com", "wrong-password"));
    }

    @Test
    void loginIssuesStaffTokenWithoutOwnerScope() {
        UserAccount staff = new UserAccount("staff@example.com", "{pbkdf2}stored-hash",
                AccountRole.STAFF, null);
        when(accounts.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));
        when(passwordEncoder.matches("secure-long-password", "{pbkdf2}stored-hash")).thenReturn(true);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(Jwt.withTokenValue("signed-test-token").header("alg", "HS256")
                        .claim("sub", "staff@example.com").build());

        assertEquals("signed-test-token", service.login("staff@example.com", "secure-long-password").accessToken());
    }
}
