package com.vetsync.api.security;

import com.vetsync.domain.model.AppointmentStatus;
import com.vetsync.domain.repository.AppointmentRepository;
import com.vetsync.domain.repository.MedicalRecordRepository;
import com.vetsync.domain.repository.PetRepository;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("accessAuthorization")
public class AccessAuthorization {
    private final PetRepository pets;
    private final MedicalRecordRepository medicalRecords;
    private final AppointmentRepository appointments;

    public AccessAuthorization(PetRepository pets, MedicalRecordRepository medicalRecords,
                               AppointmentRepository appointments) {
        this.pets = pets;
        this.medicalRecords = medicalRecords;
        this.appointments = appointments;
    }

    public boolean canAccessOwner(Authentication authentication, Long ownerId) {
        return isStaff(authentication) || Objects.equals(getOwnerId(authentication), ownerId);
    }

    public boolean canManagePetAndOwner(Authentication authentication, Long petId, Long ownerId) {
        return isStaff(authentication)
                || (Objects.equals(getOwnerId(authentication), ownerId)
                && pets.existsByIdAndOwnerId(petId, ownerId));
    }

    public boolean canAccessPet(Authentication authentication, Long petId) {
        if (isStaff(authentication)) {
            return true;
        }
        Long ownerId = getOwnerId(authentication);
        return ownerId != null && pets.existsByIdAndOwnerId(petId, ownerId);
    }

    public boolean canAccessMedicalRecord(Authentication authentication, Long recordId) {
        if (isStaff(authentication)) {
            return true;
        }
        Long ownerId = getOwnerId(authentication);
        return ownerId != null && medicalRecords.existsByIdAndOwnerId(recordId, ownerId);
    }

    public boolean canScheduleForPet(Authentication authentication, Long petId) {
        return isStaff(authentication) || canAccessPet(authentication, petId);
    }

    public boolean canRescheduleAppointment(Authentication authentication, Long appointmentId, Long petId,
                                             AppointmentStatus requestedStatus) {
        if (isStaff(authentication)) {
            return true;
        }
        Long ownerId = getOwnerId(authentication);
        return ownerId != null
                && appointments.findStatusByIdAndOwnerId(appointmentId, ownerId)
                .filter(requestedStatus::equals).isPresent()
                && pets.existsByIdAndOwnerId(petId, ownerId);
    }

    public boolean canChangeAppointmentStatus(Authentication authentication, Long appointmentId,
                                              AppointmentStatus status) {
        return isStaff(authentication)
                || (status == AppointmentStatus.CANCELLED && ownsAppointment(authentication, appointmentId));
    }

    public boolean canAccessAppointment(Authentication authentication, Long appointmentId) {
        return isStaff(authentication) || ownsAppointment(authentication, appointmentId);
    }

    public Long resolveOwnerScope(Jwt jwt, Long requestedOwnerId) {
        if ("STAFF".equals(jwt.getClaimAsString("role"))) {
            return requestedOwnerId;
        }

        Long authenticatedOwnerId = getOwnerId(jwt);
        if (authenticatedOwnerId == null
                || (requestedOwnerId != null && !requestedOwnerId.equals(authenticatedOwnerId))) {
            throw new AccessDeniedException("No tienes permiso para consultar mascotas de otro propietario");
        }
        return authenticatedOwnerId;
    }

    private boolean ownsAppointment(Authentication authentication, Long appointmentId) {
        Long ownerId = getOwnerId(authentication);
        return ownerId != null && appointments.existsByIdAndOwnerId(appointmentId, ownerId);
    }

    private boolean isStaff(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STAFF"));
    }

    private Long getOwnerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        return getOwnerId(jwt);
    }

    private Long getOwnerId(Jwt jwt) {
        Number ownerId = jwt.getClaim("ownerId");
        return ownerId == null ? null : ownerId.longValue();
    }
}
