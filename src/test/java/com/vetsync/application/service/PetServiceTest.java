package com.vetsync.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {
    @Mock
    private PetRepository pets;

    @Mock
    private OwnerRepository owners;

    @InjectMocks
    private PetService service;

    @Test
    void createRequiresAndAssociatesExistingOwner() {
        Owner owner = new Owner("Ana Lopez", "5551234567", "ana@example.com", "Calle 1");
        Pet saved = new Pet("Luna", "Perro", "Labrador", Sex.FEMALE, LocalDate.of(2022, 1, 1),
                new BigDecimal("12.500"), null, owner);
        when(owners.findById(3L)).thenReturn(Optional.of(owner));
        when(pets.save(org.mockito.ArgumentMatchers.any(Pet.class))).thenReturn(saved);

        Pet result = service.create("Luna", "Perro", "Labrador", Sex.FEMALE, LocalDate.of(2022, 1, 1),
                new BigDecimal("12.500"), null, 3L);

        assertEquals(owner, result.getOwner());
        verify(pets).save(org.mockito.ArgumentMatchers.any(Pet.class));
    }

    @Test
    void createRejectsUnknownOwner() {
        when(owners.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create("Luna", "Perro", "Labrador",
                Sex.FEMALE, LocalDate.of(2022, 1, 1), BigDecimal.ONE, null, 404L));
    }

    @Test
    void deleteRejectsPetWithMedicalHistory() {
        Pet pet = new Pet("Luna", "Perro", "Labrador", Sex.FEMALE, LocalDate.of(2022, 1, 1),
                BigDecimal.ONE, null, new Owner("Ana Lopez", "5551234567", "ana@example.com", "Calle 1"));
        when(pets.findById(5L)).thenReturn(Optional.of(pet));
        when(pets.hasMedicalRecords(5L)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> service.delete(5L));
    }

    @Test
    void searchTrimsNameAndReturnsRepositoryResults() {
        when(pets.search("Luna", null, null, null)).thenReturn(java.util.List.of());

        assertEquals(java.util.List.of(), service.search(" Luna ", "  ", null, null));
    }
}
