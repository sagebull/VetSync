package com.vetsync.domain.repository;

import com.vetsync.domain.model.MedicalRecord;
import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository {
    Optional<MedicalRecord> findById(Long id);

    boolean existsByIdAndOwnerId(Long medicalRecordId, Long ownerId);

    List<MedicalRecord> findByPetId(Long petId);

    MedicalRecord save(MedicalRecord medicalRecord);

    void delete(MedicalRecord medicalRecord);
}
