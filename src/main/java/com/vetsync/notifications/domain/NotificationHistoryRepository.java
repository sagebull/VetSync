package com.vetsync.notifications.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationHistoryRepository {
    boolean claim(Long appointmentId, long reminderMinutes, String recipientEmail, String petName,
                  String veterinarianName, LocalDateTime appointmentStart);

    boolean exists(Long appointmentId, long reminderMinutes);

    void markSent(Long appointmentId, long reminderMinutes);

    void markFailed(Long appointmentId, long reminderMinutes, String errorMessage);

    List<NotificationHistoryEntry> findAll();

    List<NotificationHistoryEntry> findByAppointmentId(Long appointmentId);
}
