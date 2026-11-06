package com.vetsync.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.MedicalRecord;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.model.Sex;
import com.vetsync.domain.repository.MedicalRecordRepository;
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
class MedicalRecordServiceTest {
    @Mock
    private MedicalRecordRepository medicalRecords;

    @Mock
    private PetRepository pets;

    @InjectMocks
    private MedicalRecordService service;

    @Test
    void createAssociatesConsultationWithPet() {
        Pet pet = new Pet("Luna", "Perro", "Labrador", Sex.FEMALE, LocalDate.of(2022, 1, 1),
                BigDecimal.ONE, null, new Owner("Ana Lopez", "5551234567", "ana@example.com", "Calle 1"));
        MedicalRecord saved = new MedicalRecord(pet, LocalDate.now(), "Revision", "Sana",
                null, null, "Rabia", null);
        when(pets.findById(2L)).thenReturn(Optional.of(pet));
        when(medicalRecords.save(org.mockito.ArgumentMatchers.any(MedicalRecord.class))).thenReturn(saved);

        MedicalRecord result = service.create(2L, LocalDate.now(), "Revision", "Sana",
                null, null, "Rabia", null);

        assertEquals("Rabia", result.getVaccines());
        verify(medicalRecords).save(org.mockito.ArgumentMatchers.any(MedicalRecord.class));
    }

    @Test
    void createRejectsUnknownPet() {
        when(pets.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(404L, LocalDate.now(), "Revision",
                null, null, null, null, null));
    }
}
