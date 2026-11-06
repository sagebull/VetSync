package com.vetsync.application.service;

import com.vetsync.application.exception.ResourceConflictException;
import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.AccountRole;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.OwnerRegistration;
import com.vetsync.domain.model.UserAccount;
import com.vetsync.domain.repository.OwnerRegistrationRepository;
import com.vetsync.domain.repository.OwnerRepository;
import com.vetsync.domain.repository.UserAccountRepository;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerRegistrationService {
    private final OwnerRegistrationRepository registrations;
    private final OwnerRepository owners;
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public OwnerRegistrationService(OwnerRegistrationRepository registrations, OwnerRepository owners,
                                    UserAccountRepository accounts, PasswordEncoder passwordEncoder, Clock clock) {
        this.registrations = registrations;
        this.owners = owners;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public OwnerRegistration submit(String fullName, String phone, String email, String address, String password) {
        String normalizedEmail = normalizeEmail(email);
        return registrations.save(new OwnerRegistration(fullName.trim(), phone.trim(), normalizedEmail,
                address.trim(), passwordEncoder.encode(password), java.time.LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<OwnerRegistration> findPending() {
        return registrations.findPending();
    }

    @Transactional
    public Owner approve(Long requestId) {
        OwnerRegistration registration = registrations.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de registro", requestId));
        if (accounts.existsByEmail(registration.getEmail())) {
            throw new ResourceConflictException("Ya existe una cuenta activa con ese correo electronico");
        }

        Owner owner = owners.save(new Owner(registration.getFullName(), registration.getPhone(),
                registration.getEmail(), registration.getAddress()));
        accounts.save(new UserAccount(registration.getEmail(), registration.getPasswordHash(),
                AccountRole.OWNER, owner));
        registrations.delete(registration);
        return owner;
    }

    @Transactional
    public void reject(Long requestId) {
        OwnerRegistration registration = registrations.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de registro", requestId));
        registrations.delete(registration);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
