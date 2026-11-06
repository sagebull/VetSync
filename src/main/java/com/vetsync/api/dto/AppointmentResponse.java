package com.vetsync.api.dto;

import com.vetsync.domain.model.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id,
        Long ownerId,
        Long petId,
        Long veterinarianId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String reason,
        AppointmentStatus status) {
}
