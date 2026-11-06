package com.vetsync.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.domain.model.Owner;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OwnerRepositoryAdapterTest {

    private final SpringDataOwnerRepository owners = mock(SpringDataOwnerRepository.class);
    private final SpringDataPetRepository pets = mock(SpringDataPetRepository.class);
    private final OwnerRepositoryAdapter adapter = new OwnerRepositoryAdapter(owners, pets);

    @Test
    void findAllReturnsOwnersFromSpringData() {
        List<Owner> found = List.of(mock(Owner.class), mock(Owner.class));
        when(owners.findAll()).thenReturn(found);

        assertThat(adapter.findAll()).isSameAs(found);
    }

    @Test
    void findByIdReturnsOwnerOrEmpty() {
        Owner owner = mock(Owner.class);
        when(owners.findById(4L)).thenReturn(Optional.of(owner));
        when(owners.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(4L)).containsSame(owner);
        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void saveReturnsPersistedOwner() {
        Owner owner = mock(Owner.class);
        when(owners.save(owner)).thenReturn(owner);

        assertThat(adapter.save(owner)).isSameAs(owner);
    }

    @Test
    void deleteDelegatesToSpringData() {
        Owner owner = mock(Owner.class);

        adapter.delete(owner);

        verify(owners).delete(owner);
    }

    @Test
    void hasPetsReportsWhetherOwnerHasPets() {
        when(pets.existsByOwner_Id(4L)).thenReturn(true);
        when(pets.existsByOwner_Id(5L)).thenReturn(false);

        assertThat(adapter.hasPets(4L)).isTrue();
        assertThat(adapter.hasPets(5L)).isFalse();
    }
}
