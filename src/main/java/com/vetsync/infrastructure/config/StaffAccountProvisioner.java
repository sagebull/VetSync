package com.vetsync.infrastructure.config;

import com.vetsync.domain.model.AccountRole;
import com.vetsync.domain.model.UserAccount;
import com.vetsync.domain.repository.UserAccountRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StaffAccountProvisioner implements ApplicationRunner {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public StaffAccountProvisioner(UserAccountRepository accounts, PasswordEncoder passwordEncoder,
                                   @Value("${vetsync.security.staff-email:}") String email,
                                   @Value("${vetsync.security.staff-password:}") String password) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("VETSYNC_STAFF_EMAIL y VETSYNC_STAFF_PASSWORD deben configurarse juntos");
        }
        if (password.length() < 16) {
            throw new IllegalStateException("La contrasena inicial del personal debe tener al menos 16 caracteres");
        }
        var existingAccount = accounts.findByEmail(email);
        if (existingAccount.isPresent()) {
            if (existingAccount.get().getRole() != AccountRole.STAFF) {
                throw new IllegalStateException("El correo configurado para personal pertenece a una cuenta de propietario");
            }
            return;
        }
        accounts.save(new UserAccount(email, passwordEncoder.encode(password), AccountRole.STAFF, null));
    }
}
