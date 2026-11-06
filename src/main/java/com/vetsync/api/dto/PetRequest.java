package com.vetsync.api.dto;

import com.vetsync.domain.model.Sex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PetRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80) String species,
        @NotBlank @Size(max = 120) String breed,
        @NotNull Sex sex,
        @NotNull @PastOrPresent LocalDate birthDate,
        @NotNull @Positive BigDecimal weight,
        @Size(max = 2000) String observations,
        @NotNull @Positive Long ownerId) {
}
