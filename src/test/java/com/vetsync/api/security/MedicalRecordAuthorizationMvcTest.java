package com.vetsync.api.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.api.controller.MedicalRecordController;
import com.vetsync.application.service.MedicalRecordService;
import com.vetsync.domain.model.MedicalRecord;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.repository.AppointmentRepository;
import com.vetsync.domain.repository.MedicalRecordRepository;
import com.vetsync.domain.repository.PetRepository;
import com.vetsync.infrastructure.config.SecurityConfiguration;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@WebMvcTest(MedicalRecordController.class)
@Import({SecurityConfiguration.class, AccessAuthorization.class})
class MedicalRecordAuthorizationMvcTest {
    private static final String TEST_JWT_SECRET = generateTestSecret();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicalRecordService medicalRecords;

    @MockitoBean
    private PetRepository pets;

    @MockitoBean
    private MedicalRecordRepository medicalRecordRepository;

    @MockitoBean
    private AppointmentRepository appointments;

    @DynamicPropertySource
    static void configureSecurity(DynamicPropertyRegistry registry) {
        registry.add("vetsync.security.jwt-secret", () -> TEST_JWT_SECRET);
    }

    @Test
    void medicalRecordEndpointRejectsRequestsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/medical-records/20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCannotReadAnotherOwnersMedicalRecord() throws Exception {
        when(medicalRecordRepository.existsByIdAndOwnerId(20L, 7L)).thenReturn(false);

        mockMvc.perform(get("/api/medical-records/20").with(ownerToken(7L)))
                .andExpect(status().isForbidden());

        verify(medicalRecords, org.mockito.Mockito.never()).get(20L);
    }

    @Test
    void ownerCanReadTheirOwnMedicalRecord() throws Exception {
        when(medicalRecordRepository.existsByIdAndOwnerId(20L, 7L)).thenReturn(true);
        MedicalRecord record = org.mockito.Mockito.mock(MedicalRecord.class);
        Pet pet = org.mockito.Mockito.mock(Pet.class);
        when(record.getPet()).thenReturn(pet);
        when(pet.getId()).thenReturn(10L);
        when(medicalRecords.get(20L)).thenReturn(record);

        mockMvc.perform(get("/api/medical-records/20").with(ownerToken(7L)))
                .andExpect(status().isOk());
    }

    @Test
    void staffCanReadMedicalRecordsWithoutOwnerScope() throws Exception {
        MedicalRecord record = org.mockito.Mockito.mock(MedicalRecord.class);
        Pet pet = org.mockito.Mockito.mock(Pet.class);
        when(record.getPet()).thenReturn(pet);
        when(pet.getId()).thenReturn(10L);
        when(medicalRecords.get(20L)).thenReturn(record);

        mockMvc.perform(get("/api/medical-records/20").with(jwt()
                        .jwt(token -> token.claim("role", "STAFF"))
                        .authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
                .andExpect(status().isOk());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor ownerToken(long ownerId) {
        return jwt().jwt(token -> token.claim("role", "OWNER").claim("ownerId", ownerId))
                .authorities(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    private static String generateTestSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }
}
