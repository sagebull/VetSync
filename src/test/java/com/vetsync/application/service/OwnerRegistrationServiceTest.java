package com.vetsync.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.AccountRole;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.OwnerRegistration;
import com.vetsync.domain.model.UserAccount;
import com.vetsync.domain.repository.OwnerRegistrationRepository;
import com.vetsync.domain.repository.OwnerRepository;
import com.vetsync.domain.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OwnerRegistrationServiceTest {
    @Mock
    private OwnerRegistrationRepository registrations;

    @Mock
    private OwnerRepository owners;

    @Mock
    private UserAccountRepository accounts;

    @Mock
    private PasswordEncoder passwordEncoder;

    private OwnerRegistrationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-05T16:00:00Z"), ZoneOffset.UTC);
        service = new OwnerRegistrationService(registrations, owners, accounts, passwordEncoder, clock);
    }

    @Test
    void submissionStoresOnlyAHashedPasswordAndDoesNotActivateAnAccount() {
        when(passwordEncoder.encode("secure-long-password")).thenReturn("hashed-password");
        when(registrations.save(any(OwnerRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OwnerRegistration result = service.submit(" Ana Lopez ", " 5551234567 ", "ANA@EXAMPLE.COM ",
                " Calle 1 ", "secure-long-password");

        assertEquals("ana@example.com", result.getEmail());
        assertEquals("hashed-password", result.getPasswordHash());
        assertEquals("2026-11-05T16:00", result.getCreatedAt().toString());
        verify(accounts, never()).save(any(UserAccount.class));
        verify(owners, never()).save(any(Owner.class));
    }

    @Test
    void approvalCreatesOwnerAndActiveAccountAndRemovesPendingCredentials() {
        OwnerRegistration registration = pendingRequest();
        when(registrations.findByIdForUpdate(4L)).thenReturn(Optional.of(registration));
        when(accounts.existsByEmail("ana@example.com")).thenReturn(false);
        when(owners.save(any(Owner.class))).thenAnswer(invocation -> {
            Owner owner = invocation.getArgument(0);
            ReflectionTestUtils.setField(owner, "id", 12L);
            return owner;
        });
        when(accounts.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Owner approved = service.approve(4L);

        assertEquals(12L, approved.getId());
        ArgumentCaptor<UserAccount> account = ArgumentCaptor.forClass(UserAccount.class);
        verify(accounts).save(account.capture());
        assertEquals(AccountRole.OWNER, account.getValue().getRole());
        assertEquals(12L, account.getValue().getOwner().getId());
        assertEquals("hashed-password", account.getValue().getPasswordHash());
        verify(registrations).delete(registration);
    }

    @Test
    void approvalRejectsUnknownPendingRequest() {
        when(registrations.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.approve(404L));

        verify(owners, never()).save(any(Owner.class));
        verify(accounts, never()).save(any(UserAccount.class));
    }

    @Test
    void rejectionDeletesPendingPersonalDataAndCredentials() {
        OwnerRegistration registration = pendingRequest();
        when(registrations.findByIdForUpdate(4L)).thenReturn(Optional.of(registration));

        service.reject(4L);

        verify(registrations).delete(registration);
        verify(owners, never()).save(any(Owner.class));
        verify(accounts, never()).save(any(UserAccount.class));
    }

    private OwnerRegistration pendingRequest() {
        OwnerRegistration registration = new OwnerRegistration("Ana Lopez", "5551234567",
                "ana@example.com", "Calle 1", "hashed-password",
                java.time.LocalDateTime.of(2026, 11, 5, 16, 0));
        ReflectionTestUtils.setField(registration, "id", 4L);
        return registration;
    }
}
