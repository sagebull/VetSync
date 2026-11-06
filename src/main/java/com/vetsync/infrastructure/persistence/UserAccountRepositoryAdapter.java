package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.UserAccount;
import com.vetsync.domain.repository.UserAccountRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountRepositoryAdapter implements UserAccountRepository {
    private final SpringDataUserAccountRepository accounts;

    public UserAccountRepositoryAdapter(SpringDataUserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return accounts.findByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return accounts.existsByEmailIgnoreCase(email);
    }

    @Override
    public UserAccount save(UserAccount account) {
        return accounts.save(account);
    }
}
