package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.MedicalRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    boolean existsByIdAndPet_Owner_Id(Long medicalRecordId, Long ownerId);

    List<MedicalRecord> findAllByPet_IdOrderByVisitDateDescIdDesc(Long petId);

    boolean existsByPet_Id(Long petId);
}
