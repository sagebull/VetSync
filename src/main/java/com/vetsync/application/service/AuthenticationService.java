package com.vetsync.application.service;

import com.vetsync.application.exception.InvalidCredentialsException;
import com.vetsync.domain.model.AccountRole;
import com.vetsync.domain.model.UserAccount;
import com.vetsync.domain.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);

    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public AuthenticationService(UserAccountRepository accounts,
                                  PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder, Clock clock) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AuthenticationToken login(String email, String password) {
        UserAccount account = accounts.findByEmail(normalizeEmail(email))
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return issueToken(account);
    }

    private AuthenticationToken issueToken(UserAccount account) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(ACCESS_TOKEN_LIFETIME);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("vetsync")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(account.getEmail())
                .claim("role", account.getRole().name());
        if (account.getRole() == AccountRole.OWNER) {
            if (account.getOwner() == null) {
                throw new IllegalStateException("Una cuenta de propietario no tiene un propietario asociado");
            }
            claims.claim("ownerId", account.getOwner().getId());
        }
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        return new AuthenticationToken(token, expiresAt);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record AuthenticationToken(String accessToken, Instant expiresAt) {
    }
}
