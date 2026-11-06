package com.vetsync.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.domain.model.Pet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PetRepositoryAdapterTest {

    private final SpringDataPetRepository pets = mock(SpringDataPetRepository.class);
    private final SpringDataMedicalRecordRepository medicalRecords = mock(SpringDataMedicalRecordRepository.class);
    private final PetRepositoryAdapter adapter = new PetRepositoryAdapter(pets, medicalRecords);

    @Test
    void findByIdReturnsPetFromSpringData() {
        Pet pet = mock(Pet.class);
        when(pets.findById(10L)).thenReturn(Optional.of(pet));

        assertThat(adapter.findById(10L)).containsSame(pet);
    }

    @Test
    void findByIdReturnsEmptyWhenPetDoesNotExist() {
        when(pets.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void existsByIdAndOwnerIdUsesOwnerScopedQuery() {
        when(pets.existsByIdAndOwner_Id(10L, 5L)).thenReturn(true);

        assertThat(adapter.existsByIdAndOwnerId(10L, 5L)).isTrue();
    }

    @Test
    void searchPassesEveryFilterToSpringData() {
        List<Pet> found = List.of(mock(Pet.class));
        when(pets.search("Luna", "Ana", 5L, 10L)).thenReturn(found);

        assertThat(adapter.search("Luna", "Ana", 5L, 10L)).isSameAs(found);
    }

    @Test
    void saveReturnsPersistedPet() {
        Pet pet = mock(Pet.class);
        when(pets.save(pet)).thenReturn(pet);

        assertThat(adapter.save(pet)).isSameAs(pet);
    }

    @Test
    void deleteDelegatesToSpringData() {
        Pet pet = mock(Pet.class);

        adapter.delete(pet);

        verify(pets).delete(pet);
    }

    @Test
    void hasMedicalRecordsReportsRecordsOfPet() {
        when(medicalRecords.existsByPet_Id(10L)).thenReturn(true);

        assertThat(adapter.hasMedicalRecords(10L)).isTrue();
    }
}
