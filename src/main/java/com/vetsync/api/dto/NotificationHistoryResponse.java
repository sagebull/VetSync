package com.vetsync.api.dto;

import com.vetsync.notifications.domain.NotificationDeliveryStatus;
import java.time.LocalDateTime;

public record NotificationHistoryResponse(
        Long id,
        Long appointmentId,
        long reminderMinutes,
        String recipientEmail,
        String petName,
        String veterinarianName,
        LocalDateTime appointmentStart,
        NotificationDeliveryStatus status,
        LocalDateTime createdAt,
        LocalDateTime sentAt,
        String errorMessage) {
}
