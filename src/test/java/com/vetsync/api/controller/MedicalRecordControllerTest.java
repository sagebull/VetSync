package com.vetsync.api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.api.exception.ApiExceptionHandler;
import com.vetsync.application.service.MedicalRecordService;
import com.vetsync.domain.model.MedicalRecord;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MedicalRecordControllerTest {

    private static final String RECORD_JSON = """
            {"visitDate":"2024-01-10","consultation":"Revision","diagnosis":"Sano",
             "treatment":"Ninguno","medication":"Ninguno","vaccines":"Rabia",
             "veterinaryObservations":"Buen estado"}
            """;

    private final MedicalRecordService medicalRecords = mock(MedicalRecordService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MedicalRecordController(medicalRecords))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    private static MedicalRecord record(long id, long petId) {
        com.vetsync.domain.model.Pet pet = mock(com.vetsync.domain.model.Pet.class);
        when(pet.getId()).thenReturn(petId);
        MedicalRecord record = mock(MedicalRecord.class);
        when(record.getId()).thenReturn(id);
        when(record.getPet()).thenReturn(pet);
        when(record.getVisitDate()).thenReturn(LocalDate.of(2030, 1, 10));
        when(record.getConsultation()).thenReturn("Revision");
        when(record.getDiagnosis()).thenReturn("Sano");
        when(record.getTreatment()).thenReturn("Ninguno");
        when(record.getMedication()).thenReturn("Ninguno");
        when(record.getVaccines()).thenReturn("Rabia");
        when(record.getVeterinaryObservations()).thenReturn("Buen estado");
        return record;
    }

    @Test
    void createReturnsMappedMedicalRecord() throws Exception {
        MedicalRecord expected = record(20L, 10L);
        when(medicalRecords.create(eq(10L), eq(LocalDate.of(2024, 1, 10)), eq("Revision"), eq("Sano"),
                eq("Ninguno"), eq("Ninguno"), eq("Rabia"), eq("Buen estado"))).thenReturn(expected);

        mockMvc.perform(post("/api/pets/10/medical-records").contentType("application/json").content(RECORD_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.petId").value(10))
                .andExpect(jsonPath("$.vaccines").value("Rabia"));
    }

    @Test
    void createRejectsBlankConsultation() throws Exception {
        mockMvc.perform(post("/api/pets/10/medical-records").contentType("application/json").content("""
                        {"visitDate":"2030-01-10","consultation":" "}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findByPetReturnsRecordsOfPet() throws Exception {
        List<MedicalRecord> expected = List.of(record(20L, 10L));
        when(medicalRecords.findByPet(10L)).thenReturn(expected);

        mockMvc.perform(get("/api/pets/10/medical-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20));
    }

    @Test
    void getReturnsRecordById() throws Exception {
        MedicalRecord expected = record(20L, 10L);
        when(medicalRecords.get(20L)).thenReturn(expected);

        mockMvc.perform(get("/api/medical-records/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Sano"));
    }

    @Test
    void updateReturnsUpdatedRecord() throws Exception {
        MedicalRecord expected = record(20L, 10L);
        when(medicalRecords.update(eq(20L), eq(LocalDate.of(2024, 1, 10)), eq("Revision"), eq("Sano"),
                eq("Ninguno"), eq("Ninguno"), eq("Rabia"), eq("Buen estado"))).thenReturn(expected);

        mockMvc.perform(put("/api/medical-records/20").contentType("application/json").content(RECORD_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void deleteInvokesServiceAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/medical-records/20"))
                .andExpect(status().isNoContent());

        verify(medicalRecords).delete(20L);
    }
}
