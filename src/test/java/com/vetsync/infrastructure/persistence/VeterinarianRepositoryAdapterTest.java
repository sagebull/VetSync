package com.vetsync.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsync.domain.model.Veterinarian;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VeterinarianRepositoryAdapterTest {

    private final SpringDataVeterinarianRepository veterinarians = mock(SpringDataVeterinarianRepository.class);
    private final VeterinarianRepositoryAdapter adapter = new VeterinarianRepositoryAdapter(veterinarians);

    @Test
    void findByIdReturnsVeterinarianOrEmpty() {
        Veterinarian vet = mock(Veterinarian.class);
        when(veterinarians.findById(3L)).thenReturn(Optional.of(vet));
        when(veterinarians.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(3L)).containsSame(vet);
        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void findByIdForUpdateReturnsLockedVeterinarianOrEmpty() {
        Veterinarian vet = mock(Veterinarian.class);
        when(veterinarians.findByIdForUpdate(3L)).thenReturn(Optional.of(vet));
        when(veterinarians.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findByIdForUpdate(3L)).containsSame(vet);
        assertThat(adapter.findByIdForUpdate(99L)).isEmpty();
    }

    @Test
    void findAllReturnsVeterinariansFromSpringData() {
        List<Veterinarian> found = List.of(mock(Veterinarian.class));
        when(veterinarians.findAll()).thenReturn(found);

        assertThat(adapter.findAll()).isSameAs(found);
    }

    @Test
    void saveReturnsPersistedVeterinarian() {
        Veterinarian vet = mock(Veterinarian.class);
        when(veterinarians.save(vet)).thenReturn(vet);

        assertThat(adapter.save(vet)).isSameAs(vet);
    }
}
