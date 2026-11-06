package com.vetsync.api.dto;

import java.time.LocalDateTime;

public record OwnerRegistrationPendingResponse(
        Long requestId,
        String fullName,
        String phone,
        String email,
        String address,
        LocalDateTime createdAt) {
}
