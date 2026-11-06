package com.vetsync.api.controller;

import com.vetsync.api.dto.MedicalRecordRequest;
import com.vetsync.api.dto.MedicalRecordResponse;
import com.vetsync.application.service.MedicalRecordService;
import com.vetsync.domain.model.MedicalRecord;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class MedicalRecordController {
    private final MedicalRecordService medicalRecords;

    public MedicalRecordController(MedicalRecordService medicalRecords) {
        this.medicalRecords = medicalRecords;
    }

    @PostMapping("/api/pets/{petId}/medical-records")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STAFF')")
    public MedicalRecordResponse create(@PathVariable Long petId, @Valid @RequestBody MedicalRecordRequest request) {
        return toResponse(medicalRecords.create(petId, request.visitDate(), request.consultation(), request.diagnosis(),
                request.treatment(), request.medication(), request.vaccines(), request.veterinaryObservations()));
    }

    @GetMapping("/api/pets/{petId}/medical-records")
    @PreAuthorize("@accessAuthorization.canAccessPet(authentication, #petId)")
    public List<MedicalRecordResponse> findByPet(@PathVariable Long petId) {
        return medicalRecords.findByPet(petId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/api/medical-records/{id}")
    @PreAuthorize("@accessAuthorization.canAccessMedicalRecord(authentication, #id)")
    public MedicalRecordResponse get(@PathVariable Long id) {
        return toResponse(medicalRecords.get(id));
    }

    @PutMapping("/api/medical-records/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public MedicalRecordResponse update(@PathVariable Long id, @Valid @RequestBody MedicalRecordRequest request) {
        return toResponse(medicalRecords.update(id, request.visitDate(), request.consultation(), request.diagnosis(),
                request.treatment(), request.medication(), request.vaccines(), request.veterinaryObservations()));
    }

    @DeleteMapping("/api/medical-records/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('STAFF')")
    public void delete(@PathVariable Long id) {
        medicalRecords.delete(id);
    }

    private MedicalRecordResponse toResponse(MedicalRecord record) {
        return new MedicalRecordResponse(record.getId(), record.getPet().getId(), record.getVisitDate(),
                record.getConsultation(), record.getDiagnosis(), record.getTreatment(), record.getMedication(),
                record.getVaccines(), record.getVeterinaryObservations());
    }
}
