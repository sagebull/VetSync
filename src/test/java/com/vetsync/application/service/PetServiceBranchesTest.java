package com.vetsync.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.application.exception.ResourceConflictException;
import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.model.Sex;
import com.vetsync.domain.repository.OwnerRepository;
import com.vetsync.domain.repository.PetRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PetServiceBranchesTest {

    private static final LocalDate BIRTH = LocalDate.of(2020, 5, 1);

    @Test
    void createAttachesPetToExistingOwnerAndSaves() {
        PetRepository pets = mock(PetRepository.class);
        OwnerRepository owners = mock(OwnerRepository.class);
        Owner owner = new Owner("Maria Lopez", "600", "maria@mail.com", "Calle 1");
        Pet saved = new Pet("Luna", "Perro", "Mestizo", Sex.FEMALE, BIRTH, BigDecimal.TEN, "obs", owner);
        when(owners.findById(7L)).thenReturn(Optional.of(owner));
        when(pets.save(any(Pet.class))).thenReturn(saved);
        PetService service = new PetService(pets, owners);

        Pet result = service.create("Luna", "Perro", "Mestizo", Sex.FEMALE, BIRTH, BigDecimal.TEN, "obs", 7L);

        assertThat(result).isSameAs(saved);
    }

    @Test
    void createFailsWhenOwnerDoesNotExist() {
        PetRepository pets = mock(PetRepository.class);
        OwnerRepository owners = mock(OwnerRepository.class);
        when(owners.findById(404L)).thenReturn(Optional.empty());
        PetService service = new PetService(pets, owners);

        assertThatThrownBy(() -> service.create("Luna", "Perro", null, Sex.FEMALE, BIRTH, null, null, 404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
        verify(pets, never()).save(any());
    }

    @Test
    void getFailsWhenPetMissing() {
        PetRepository pets = mock(PetRepository.class);
        when(pets.findById(5L)).thenReturn(Optional.empty());
        PetService service = new PetService(pets, mock(OwnerRepository.class));

        assertThatThrownBy(() -> service.get(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchNormalizesBlankAndPaddedFilters() {
        PetRepository pets = mock(PetRepository.class);
        List<Pet> expected = List.of();
        when(pets.search("Luna", null, 2L, null)).thenReturn(expected);
        PetService service = new PetService(pets, mock(OwnerRepository.class));

        assertThat(service.search("  Luna  ", "   ", 2L, null)).isSameAs(expected);
    }

    @Test
    void updateFailsWhenNewOwnerMissingWithoutSaving() {
        PetRepository pets = mock(PetRepository.class);
        OwnerRepository owners = mock(OwnerRepository.class);
        Pet pet = new Pet("Luna", "Perro", null, Sex.FEMALE, BIRTH, null, null, null);
        when(pets.findById(5L)).thenReturn(Optional.of(pet));
        when(owners.findById(8L)).thenReturn(Optional.empty());
        PetService service = new PetService(pets, owners);

        assertThatThrownBy(() -> service.update(5L, "Nala", "Gato", null, Sex.MALE, BIRTH, null, null, 8L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Propietario");
        verify(pets, never()).save(any());
    }

    @Test
    void deleteRejectsPetWithMedicalHistory() {
        PetRepository pets = mock(PetRepository.class);
        Pet pet = new Pet("Luna", "Perro", null, Sex.FEMALE, BIRTH, null, null, null);
        when(pets.findById(5L)).thenReturn(Optional.of(pet));
        when(pets.hasMedicalRecords(5L)).thenReturn(true);
        PetService service = new PetService(pets, mock(OwnerRepository.class));

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("historial");
        verify(pets, never()).delete(any());
    }

    @Test
    void deleteFailsWhenPetMissing() {
        PetRepository pets = mock(PetRepository.class);
        when(pets.findById(5L)).thenReturn(Optional.empty());
        PetService service = new PetService(pets, mock(OwnerRepository.class));

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(ResourceNotFoundException.class);
        verify(pets, never()).delete(any());
    }
}
