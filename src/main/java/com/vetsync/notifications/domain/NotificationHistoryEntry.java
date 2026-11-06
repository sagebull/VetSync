package com.vetsync.notifications.domain;

import java.time.LocalDateTime;

public record NotificationHistoryEntry(
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
