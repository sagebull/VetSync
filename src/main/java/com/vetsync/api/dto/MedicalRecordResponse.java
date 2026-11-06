package com.vetsync.api.dto;

import java.time.LocalDate;

public record MedicalRecordResponse(Long id, Long petId, LocalDate visitDate, String consultation,
                                    String diagnosis, String treatment, String medication,
                                    String vaccines, String veterinaryObservations) {
}
