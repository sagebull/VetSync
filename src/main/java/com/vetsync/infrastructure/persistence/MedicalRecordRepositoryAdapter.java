package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.MedicalRecord;
import com.vetsync.domain.repository.MedicalRecordRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MedicalRecordRepositoryAdapter implements MedicalRecordRepository {
    private final SpringDataMedicalRecordRepository medicalRecords;

    public MedicalRecordRepositoryAdapter(SpringDataMedicalRecordRepository medicalRecords) {
        this.medicalRecords = medicalRecords;
    }

    @Override
    public Optional<MedicalRecord> findById(Long id) {
        return medicalRecords.findById(id);
    }

    @Override
    public boolean existsByIdAndOwnerId(Long medicalRecordId, Long ownerId) {
        return medicalRecords.existsByIdAndPet_Owner_Id(medicalRecordId, ownerId);
    }

    @Override
    public List<MedicalRecord> findByPetId(Long petId) {
        return medicalRecords.findAllByPet_IdOrderByVisitDateDescIdDesc(petId);
    }

    @Override
    public MedicalRecord save(MedicalRecord medicalRecord) {
        return medicalRecords.save(medicalRecord);
    }

    @Override
    public void delete(MedicalRecord medicalRecord) {
        medicalRecords.delete(medicalRecord);
    }
}
