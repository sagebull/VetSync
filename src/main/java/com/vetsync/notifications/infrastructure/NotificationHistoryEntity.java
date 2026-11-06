package com.vetsync.notifications.infrastructure;

import com.vetsync.notifications.domain.NotificationDeliveryStatus;
import com.vetsync.notifications.domain.NotificationHistoryEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_history")
public class NotificationHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "reminder_minutes", nullable = false)
    private long reminderMinutes;

    @Column(name = "recipient_email", nullable = false, length = 254)
    private String recipientEmail;

    @Column(name = "pet_name", nullable = false, length = 120)
    private String petName;

    @Column(name = "veterinarian_name", nullable = false, length = 160)
    private String veterinarianName;

    @Column(name = "appointment_start", nullable = false)
    private LocalDateTime appointmentStart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationDeliveryStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public NotificationHistoryEntity() {
    }

    NotificationHistoryEntry toEntry() {
        return new NotificationHistoryEntry(id, appointmentId, reminderMinutes, recipientEmail,
                petName, veterinarianName, appointmentStart, status, createdAt, sentAt, errorMessage);
    }
}
