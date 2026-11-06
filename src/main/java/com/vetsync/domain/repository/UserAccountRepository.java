package com.vetsync.domain.repository;

import com.vetsync.domain.model.UserAccount;
import java.util.Optional;

public interface UserAccountRepository {
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    UserAccount save(UserAccount account);
}
