package com.vetsync.api.dto;

import com.vetsync.domain.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record AppointmentStatusRequest(@NotNull AppointmentStatus status) {
}
