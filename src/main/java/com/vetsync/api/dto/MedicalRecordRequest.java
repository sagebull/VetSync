package com.vetsync.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MedicalRecordRequest(
        @NotNull @PastOrPresent LocalDate visitDate,
        @NotBlank @Size(max = 2000) String consultation,
        @Size(max = 2000) String diagnosis,
        @Size(max = 2000) String treatment,
        @Size(max = 2000) String medication,
        @Size(max = 2000) String vaccines,
        @Size(max = 4000) String veterinaryObservations) {
}
