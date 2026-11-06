package com.vetsync.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsync.domain.model.AccountRole;
import com.vetsync.domain.model.UserAccount;
import com.vetsync.domain.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class StaffAccountProvisionerTest {

    private static final String LONG_PASSWORD = "0123456789abcdef";

    @Test
    void doesNothingWhenEmailAndPasswordAreBlank() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        StaffAccountProvisioner provisioner = new StaffAccountProvisioner(accounts, mock(PasswordEncoder.class), "  ", "");

        provisioner.run(new DefaultApplicationArguments());

        verifyNoInteractions(accounts);
    }

    @Test
    void rejectsEmailWithoutPassword() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        StaffAccountProvisioner provisioner = new StaffAccountProvisioner(accounts, mock(PasswordEncoder.class), "staff@vet.com", "");

        assertThatThrownBy(() -> provisioner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deben configurarse juntos");
        verifyNoInteractions(accounts);
    }

    @Test
    void rejectsPasswordWithoutEmail() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        StaffAccountProvisioner provisioner = new StaffAccountProvisioner(accounts, mock(PasswordEncoder.class), "", LONG_PASSWORD);

        assertThatThrownBy(() -> provisioner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deben configurarse juntos");
        verifyNoInteractions(accounts);
    }

    @Test
    void rejectsShortInitialPassword() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        StaffAccountProvisioner provisioner = new StaffAccountProvisioner(accounts, mock(PasswordEncoder.class), "staff@vet.com", "short");

        assertThatThrownBy(() -> provisioner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("al menos 16 caracteres");
        verify(accounts, never()).save(any());
    }

    @Test
    void createsStaffAccountWithEncodedPasswordUsingNormalizedEmail() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(accounts.findByEmail("staff@vet.com")).thenReturn(Optional.empty());
        when(encoder.encode(LONG_PASSWORD)).thenReturn("hashed-value");
        StaffAccountProvisioner provisioner = new StaffAccountProvisioner(accounts, encoder, "  Staff@VET.com ", LONG_PASSWORD);

        provisioner.run(new DefaultApplicationArguments());

        verify(accounts).save(argThatAccount("staff@vet.com", "hashed-value", AccountRole.STAFF));
    }

    @Test
    void keepsExistingStaffAccountUntouched() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserAccount existing = new UserAccount("staff@vet.com", "hash", AccountRole.STAFF, null);
        when(accounts.findByEmail("staff@vet.com")).thenReturn(Optional.of(existing));
        StaffAccountProvisioner provisioner = new StaffAccountProvisioner(accounts, mock(PasswordEncoder.class), "staff@vet.com", LONG_PASSWORD);

        provisioner.run(new DefaultApplicationArguments());

        verify(accounts, never()).save(any());
    }

    @Test
    void refusesToPromoteOwnerAccountToStaff() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        UserAccount ownerAccount = new UserAccount("owner@vet.com", "hash", AccountRole.OWNER, null);
        when(accounts.findByEmail("owner@vet.com")).thenReturn(Optional.of(ownerAccount));
        StaffAccountProvisioner provisioner = new StaffAccountProvisioner(accounts, mock(PasswordEncoder.class), "owner@vet.com", LONG_PASSWORD);

        assertThatThrownBy(() -> provisioner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pertenece a una cuenta de propietario");
        verify(accounts, never()).save(any());
    }

    private static UserAccount argThatAccount(String email, String hash, AccountRole role) {
        return org.mockito.ArgumentMatchers.argThat(account ->
                account != null
                        && email.equals(account.getEmail())
                        && hash.equals(account.getPasswordHash())
                        && role == account.getRole());
    }
}
