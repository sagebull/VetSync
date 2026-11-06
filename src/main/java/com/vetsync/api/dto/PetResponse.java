package com.vetsync.api.dto;

import com.vetsync.domain.model.Sex;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PetResponse(Long id, String name, String species, String breed, Sex sex,
                          LocalDate birthDate, BigDecimal weight, String observations, Long ownerId) {
}
