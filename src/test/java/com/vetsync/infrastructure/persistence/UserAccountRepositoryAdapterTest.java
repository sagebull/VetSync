package com.vetsync.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsync.domain.model.UserAccount;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserAccountRepositoryAdapterTest {

    private final SpringDataUserAccountRepository accounts = mock(SpringDataUserAccountRepository.class);
    private final UserAccountRepositoryAdapter adapter = new UserAccountRepositoryAdapter(accounts);

    @Test
    void findByEmailIsCaseInsensitive() {
        UserAccount account = mock(UserAccount.class);
        when(accounts.findByEmailIgnoreCase("ANA@example.com")).thenReturn(Optional.of(account));

        assertThat(adapter.findByEmail("ANA@example.com")).containsSame(account);
    }

    @Test
    void findByEmailReturnsEmptyWhenAccountIsMissing() {
        when(accounts.findByEmailIgnoreCase("nadie@example.com")).thenReturn(Optional.empty());

        assertThat(adapter.findByEmail("nadie@example.com")).isEmpty();
    }

    @Test
    void existsByEmailIsCaseInsensitive() {
        when(accounts.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

        assertThat(adapter.existsByEmail("ana@example.com")).isTrue();
    }

    @Test
    void saveReturnsPersistedAccount() {
        UserAccount account = mock(UserAccount.class);
        when(accounts.save(account)).thenReturn(account);

        assertThat(adapter.save(account)).isSameAs(account);
    }
}
