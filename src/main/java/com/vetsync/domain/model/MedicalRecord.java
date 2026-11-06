package com.vetsync.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false, foreignKey = @ForeignKey(name = "fk_medical_records_pet"))
    private Pet pet;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(nullable = false, length = 2000)
    private String consultation;

    @Column(length = 2000)
    private String diagnosis;

    @Column(length = 2000)
    private String treatment;

    @Column(length = 2000)
    private String medication;

    @Column(length = 2000)
    private String vaccines;

    @Column(name = "veterinary_observations", length = 4000)
    private String veterinaryObservations;

    protected MedicalRecord() {
    }

    public MedicalRecord(Pet pet, LocalDate visitDate, String consultation, String diagnosis,
                         String treatment, String medication, String vaccines, String veterinaryObservations) {
        this.pet = pet;
        update(visitDate, consultation, diagnosis, treatment, medication, vaccines, veterinaryObservations);
    }

    public void update(LocalDate visitDate, String consultation, String diagnosis, String treatment,
                       String medication, String vaccines, String veterinaryObservations) {
        this.visitDate = visitDate;
        this.consultation = consultation;
        this.diagnosis = diagnosis;
        this.treatment = treatment;
        this.medication = medication;
        this.vaccines = vaccines;
        this.veterinaryObservations = veterinaryObservations;
    }

    public Long getId() {
        return id;
    }

    public Pet getPet() {
        return pet;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public String getConsultation() {
        return consultation;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public String getMedication() {
        return medication;
    }

    public String getVaccines() {
        return vaccines;
    }

    public String getVeterinaryObservations() {
        return veterinaryObservations;
    }
}
