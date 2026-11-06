package com.vetsync.api.dto;

import com.vetsync.domain.model.AppointmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateAppointmentRequest(
        @NotNull @Positive Long petId,
        @NotNull @Positive Long veterinarianId,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotBlank @Size(max = 1000) String reason,
        @NotNull AppointmentStatus status) {
}
