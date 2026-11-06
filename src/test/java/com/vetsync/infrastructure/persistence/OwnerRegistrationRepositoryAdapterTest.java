package com.vetsync.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.domain.model.OwnerRegistration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OwnerRegistrationRepositoryAdapterTest {

    private final SpringDataOwnerRegistrationRepository registrations = mock(SpringDataOwnerRegistrationRepository.class);
    private final OwnerRegistrationRepositoryAdapter adapter = new OwnerRegistrationRepositoryAdapter(registrations);

    @Test
    void saveReturnsPersistedRequest() {
        OwnerRegistration request = mock(OwnerRegistration.class);
        when(registrations.save(request)).thenReturn(request);

        assertThat(adapter.save(request)).isSameAs(request);
    }

    @Test
    void findPendingReturnsRequestsOrderedByCreationFromSpringData() {
        List<OwnerRegistration> found = List.of(mock(OwnerRegistration.class));
        when(registrations.findAllByOrderByCreatedAtAsc()).thenReturn(found);

        assertThat(adapter.findPending()).isSameAs(found);
    }

    @Test
    void findByIdForUpdateReturnsRequestOrEmpty() {
        OwnerRegistration request = mock(OwnerRegistration.class);
        when(registrations.findByIdForUpdate(8L)).thenReturn(Optional.of(request));
        when(registrations.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findByIdForUpdate(8L)).containsSame(request);
        assertThat(adapter.findByIdForUpdate(99L)).isEmpty();
    }

    @Test
    void deleteDelegatesToSpringData() {
        OwnerRegistration request = mock(OwnerRegistration.class);

        adapter.delete(request);

        verify(registrations).delete(request);
    }
}
