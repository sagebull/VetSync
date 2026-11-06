package com.vetsync.notifications.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataNotificationHistoryRepository extends JpaRepository<NotificationHistoryEntity, Long> {
    boolean existsByAppointmentIdAndReminderMinutes(Long appointmentId, long reminderMinutes);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO notification_history
                (appointment_id, reminder_minutes, recipient_email, pet_name, veterinarian_name,
                 appointment_start, status, created_at, updated_at)
            VALUES
                (:appointmentId, :reminderMinutes, :recipientEmail, :petName, :veterinarianName,
                 :appointmentStart, 'PROCESSING', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            """, nativeQuery = true)
    int claim(@Param("appointmentId") Long appointmentId,
              @Param("reminderMinutes") long reminderMinutes,
              @Param("recipientEmail") String recipientEmail,
              @Param("petName") String petName,
              @Param("veterinarianName") String veterinarianName,
              @Param("appointmentStart") LocalDateTime appointmentStart);

    @Modifying
    @Query(value = """
            UPDATE notification_history
            SET status = 'SENT', sent_at = CURRENT_TIMESTAMP(6), updated_at = CURRENT_TIMESTAMP(6),
                error_message = NULL
            WHERE appointment_id = :appointmentId AND reminder_minutes = :reminderMinutes
            """, nativeQuery = true)
    int markSent(@Param("appointmentId") Long appointmentId, @Param("reminderMinutes") long reminderMinutes);

    @Modifying
    @Query(value = """
            UPDATE notification_history
            SET status = 'FAILED', error_message = :errorMessage, updated_at = CURRENT_TIMESTAMP(6)
            WHERE appointment_id = :appointmentId AND reminder_minutes = :reminderMinutes
            """, nativeQuery = true)
    int markFailed(@Param("appointmentId") Long appointmentId,
                   @Param("reminderMinutes") long reminderMinutes,
                   @Param("errorMessage") String errorMessage);

    List<NotificationHistoryEntity> findAllByOrderByCreatedAtDesc();

    List<NotificationHistoryEntity> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);
}
