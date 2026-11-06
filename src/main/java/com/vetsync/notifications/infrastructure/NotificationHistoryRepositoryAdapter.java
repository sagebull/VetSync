package com.vetsync.notifications.infrastructure;

import com.vetsync.notifications.domain.NotificationHistoryEntry;
import com.vetsync.notifications.domain.NotificationHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationHistoryRepositoryAdapter implements NotificationHistoryRepository {
    private final SpringDataNotificationHistoryRepository history;

    public NotificationHistoryRepositoryAdapter(SpringDataNotificationHistoryRepository history) {
        this.history = history;
    }

    @Override
    public boolean claim(Long appointmentId, long reminderMinutes, String recipientEmail, String petName,
                         String veterinarianName, LocalDateTime appointmentStart) {
        int inserted = history.claim(appointmentId, reminderMinutes, recipientEmail, petName,
                veterinarianName, appointmentStart);
        if (inserted == 1) {
            return true;
        }
        if (inserted == 0 && history.existsByAppointmentIdAndReminderMinutes(appointmentId, reminderMinutes)) {
            return false;
        }
        throw new IllegalStateException("No se pudo reservar el registro de notificacion");
    }

    @Override
    public boolean exists(Long appointmentId, long reminderMinutes) {
        return history.existsByAppointmentIdAndReminderMinutes(appointmentId, reminderMinutes);
    }

    @Override
    public void markSent(Long appointmentId, long reminderMinutes) {
        requireUpdated(history.markSent(appointmentId, reminderMinutes));
    }

    @Override
    public void markFailed(Long appointmentId, long reminderMinutes, String errorMessage) {
        requireUpdated(history.markFailed(appointmentId, reminderMinutes, errorMessage));
    }

    @Override
    public List<NotificationHistoryEntry> findAll() {
        return history.findAllByOrderByCreatedAtDesc().stream()
                .map(NotificationHistoryEntity::toEntry).toList();
    }

    @Override
    public List<NotificationHistoryEntry> findByAppointmentId(Long appointmentId) {
        return history.findByAppointmentIdOrderByCreatedAtDesc(appointmentId).stream()
                .map(NotificationHistoryEntity::toEntry).toList();
    }

    private void requireUpdated(int updatedRows) {
        if (updatedRows != 1) {
            throw new IllegalStateException("No se pudo actualizar el estado del historial de notificacion");
        }
    }
}
