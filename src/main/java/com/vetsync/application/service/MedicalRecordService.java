package com.vetsync.application.service;

import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.MedicalRecord;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.repository.MedicalRecordRepository;
import com.vetsync.domain.repository.PetRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalRecordService {
    private final MedicalRecordRepository medicalRecords;
    private final PetRepository pets;

    public MedicalRecordService(MedicalRecordRepository medicalRecords, PetRepository pets) {
        this.medicalRecords = medicalRecords;
        this.pets = pets;
    }

    @Transactional
    public MedicalRecord create(Long petId, LocalDate visitDate, String consultation, String diagnosis,
                                String treatment, String medication, String vaccines, String veterinaryObservations) {
        Pet pet = findPet(petId);
        return medicalRecords.save(new MedicalRecord(pet, visitDate, consultation, diagnosis, treatment,
                medication, vaccines, veterinaryObservations));
    }

    @Transactional(readOnly = true)
    public List<MedicalRecord> findByPet(Long petId) {
        findPet(petId);
        return medicalRecords.findByPetId(petId);
    }

    @Transactional(readOnly = true)
    public MedicalRecord get(Long id) {
        return medicalRecords.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro medico", id));
    }

    @Transactional
    public MedicalRecord update(Long id, LocalDate visitDate, String consultation, String diagnosis,
                                String treatment, String medication, String vaccines, String veterinaryObservations) {
        MedicalRecord record = medicalRecords.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro medico", id));
        record.update(visitDate, consultation, diagnosis, treatment, medication, vaccines, veterinaryObservations);
        return medicalRecords.save(record);
    }

    @Transactional
    public void delete(Long id) {
        MedicalRecord record = medicalRecords.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro medico", id));
        medicalRecords.delete(record);
    }

    private Pet findPet(Long id) {
        return pets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mascota", id));
    }
}
