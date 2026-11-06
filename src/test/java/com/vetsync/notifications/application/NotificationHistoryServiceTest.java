package com.vetsync.notifications.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.repository.AppointmentRepository;
import com.vetsync.notifications.domain.NotificationDeliveryStatus;
import com.vetsync.notifications.domain.NotificationHistoryEntry;
import com.vetsync.notifications.domain.NotificationHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationHistoryServiceTest {
    @Mock
    private AppointmentRepository appointments;

    @Mock
    private NotificationHistoryRepository history;

    @InjectMocks
    private NotificationHistoryService service;

    @Test
    void returnsNotificationHistoryForAnExistingAppointment() {
        NotificationHistoryEntry entry = new NotificationHistoryEntry(
                1L, 7L, 60, "ana@example.com", "Luna", "Dra. Silva",
                LocalDateTime.of(2026, 11, 6, 9, 0), NotificationDeliveryStatus.SENT,
                LocalDateTime.of(2026, 11, 6, 8, 0), LocalDateTime.of(2026, 11, 6, 8, 0), null);
        when(appointments.findById(7L)).thenReturn(Optional.of(org.mockito.Mockito.mock(Appointment.class)));
        when(history.findByAppointmentId(7L)).thenReturn(List.of(entry));

        assertEquals(List.of(entry), service.findByAppointment(7L));
        verify(history).findByAppointmentId(7L);
    }

    @Test
    void rejectsHistoryLookupForUnknownAppointment() {
        when(appointments.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByAppointment(404L));
    }
}
