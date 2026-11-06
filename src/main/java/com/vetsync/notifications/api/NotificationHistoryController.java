package com.vetsync.notifications.api;

import com.vetsync.api.dto.NotificationHistoryResponse;
import com.vetsync.notifications.application.NotificationHistoryService;
import com.vetsync.notifications.domain.NotificationHistoryEntry;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@Validated
@RestController
@RequestMapping("/api/appointments/{appointmentId}/notifications")
public class NotificationHistoryController {
    private final NotificationHistoryService history;

    public NotificationHistoryController(NotificationHistoryService history) {
        this.history = history;
    }

    @GetMapping
    @PreAuthorize("@accessAuthorization.canAccessAppointment(authentication, #appointmentId)")
    public List<NotificationHistoryResponse> findByAppointment(
            @PathVariable @Positive Long appointmentId) {
        return history.findByAppointment(appointmentId).stream().map(this::toResponse).toList();
    }

    private NotificationHistoryResponse toResponse(NotificationHistoryEntry entry) {
        return new NotificationHistoryResponse(entry.id(), entry.appointmentId(), entry.reminderMinutes(),
                entry.recipientEmail(), entry.petName(), entry.veterinarianName(), entry.appointmentStart(),
                entry.status(), entry.createdAt(), entry.sentAt(), entry.errorMessage());
    }
}
