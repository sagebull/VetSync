package com.vetsync.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.vetsync.domain.model.AppointmentStatus;
import com.vetsync.domain.repository.AppointmentRepository;
import com.vetsync.domain.repository.MedicalRecordRepository;
import com.vetsync.domain.repository.PetRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AccessAuthorizationTest {
    @Mock
    private PetRepository pets;

    @Mock
    private MedicalRecordRepository medicalRecords;

    @Mock
    private AppointmentRepository appointments;

    private AccessAuthorization authorization;

    @BeforeEach
    void setUp() {
        authorization = new AccessAuthorization(pets, medicalRecords, appointments);
    }

    @Test
    void ownerCanAccessOnlyTheirOwnOwnerRecord() {
        assertTrue(authorization.canAccessOwner(ownerAuthentication(7L), 7L));
        assertFalse(authorization.canAccessOwner(ownerAuthentication(7L), 8L));
    }

    @Test
    void ownerCanAccessOnlyTheirOwnPetsAndMedicalRecords() {
        when(pets.existsByIdAndOwnerId(10L, 7L)).thenReturn(true);
        when(pets.existsByIdAndOwnerId(11L, 7L)).thenReturn(false);
        when(medicalRecords.existsByIdAndOwnerId(20L, 7L)).thenReturn(true);

        assertTrue(authorization.canAccessPet(ownerAuthentication(7L), 10L));
        assertFalse(authorization.canAccessPet(ownerAuthentication(7L), 11L));
        assertTrue(authorization.canAccessMedicalRecord(ownerAuthentication(7L), 20L));
    }

    @Test
    void staffCanAccessRecordsWithoutOwnerScopeChecks() {
        var staff = staffAuthentication();

        assertTrue(authorization.canAccessOwner(staff, 8L));
        assertTrue(authorization.canAccessPet(staff, 11L));
        assertTrue(authorization.canAccessMedicalRecord(staff, 20L));
    }

    @Test
    void ownerCannotChangePetOwnershipOrManageSomebodyElsesAppointment() {
        var owner = ownerAuthentication(7L);
        when(pets.existsByIdAndOwnerId(10L, 7L)).thenReturn(true);
        when(appointments.existsByIdAndOwnerId(30L, 7L)).thenReturn(false);

        assertTrue(authorization.canManagePetAndOwner(owner, 10L, 7L));
        assertFalse(authorization.canManagePetAndOwner(owner, 10L, 8L));
        assertFalse(authorization.canRescheduleAppointment(owner, 30L, 10L, AppointmentStatus.PENDING));
        assertFalse(authorization.canChangeAppointmentStatus(owner, 30L, AppointmentStatus.CANCELLED));
        assertFalse(authorization.canChangeAppointmentStatus(owner, 30L, AppointmentStatus.CONFIRMED));
    }

    @Test
    void ownerCanOnlyCancelTheirOwnAppointment() {
        var owner = ownerAuthentication(7L);
        when(appointments.existsByIdAndOwnerId(30L, 7L)).thenReturn(true);
        when(appointments.findStatusByIdAndOwnerId(30L, 7L)).thenReturn(java.util.Optional.of(
                AppointmentStatus.CONFIRMED));
        when(pets.existsByIdAndOwnerId(10L, 7L)).thenReturn(true);

        assertTrue(authorization.canRescheduleAppointment(owner, 30L, 10L, AppointmentStatus.CONFIRMED));
        assertFalse(authorization.canRescheduleAppointment(owner, 30L, 10L, AppointmentStatus.COMPLETED));
        assertTrue(authorization.canChangeAppointmentStatus(owner, 30L, AppointmentStatus.CANCELLED));
        assertTrue(authorization.canAccessAppointment(owner, 30L));
        assertFalse(authorization.canChangeAppointmentStatus(owner, 30L, AppointmentStatus.COMPLETED));
    }

    @Test
    void ownerSearchIsAlwaysRestrictedToTheirOwnRecords() {
        var ownerJwt = ownerJwt(7L);

        assertEquals(7L, authorization.resolveOwnerScope(ownerJwt, null));
        assertEquals(7L, authorization.resolveOwnerScope(ownerJwt, 7L));
        assertThrows(AccessDeniedException.class, () -> authorization.resolveOwnerScope(ownerJwt, 8L));
    }

    @Test
    void staffCanSearchAcrossOwnersOrApplyAnExplicitOwnerFilter() {
        assertEquals(null, authorization.resolveOwnerScope(staffJwt(), null));
        assertEquals(8L, authorization.resolveOwnerScope(staffJwt(), 8L));
    }

    @Test
    void invalidAuthenticationHasNoOwnerAccess() {
        var anonymous = new UsernamePasswordAuthenticationToken(
                "anonymous", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

        assertFalse(authorization.canAccessOwner(anonymous, 7L));
    }

    private JwtAuthenticationToken ownerAuthentication(long ownerId) {
        return new JwtAuthenticationToken(ownerJwt(ownerId),
                AuthorityUtils.createAuthorityList("ROLE_OWNER"));
    }

    private JwtAuthenticationToken staffAuthentication() {
        return new JwtAuthenticationToken(staffJwt(),
                AuthorityUtils.createAuthorityList("ROLE_STAFF"));
    }

    private Jwt ownerJwt(long ownerId) {
        return Jwt.withTokenValue("test-owner-token").header("alg", "HS256")
                .claim("role", "OWNER").claim("ownerId", ownerId).build();
    }

    private Jwt staffJwt() {
        return Jwt.withTokenValue("test-staff-token").header("alg", "HS256")
                .claim("role", "STAFF").build();
    }
}
