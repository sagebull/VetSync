package com.vetsync.notifications.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.model.AppointmentStatus;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.model.Sex;
import com.vetsync.domain.model.Veterinarian;
import com.vetsync.domain.repository.AppointmentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReminderProcessingServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 5, 16, 0);

    @Mock
    private AppointmentRepository appointments;

    @Mock
    private NotificationDeliveryService delivery;

    private ReminderProcessingService service;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties(true, 60, Duration.ofMinutes(1));
        Clock clock = Clock.fixed(Instant.parse("2026-11-05T16:00:00Z"), ZoneOffset.UTC);
        service = new ReminderProcessingService(appointments, delivery, properties, clock);
    }

    @Test
    void processesAppointmentsInTheConfiguredDueWindow() {
        Appointment appointment = appointment(21L, LocalDateTime.of(2026, 11, 5, 16, 59));
        when(appointments.findReminderCandidates(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 5),
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)))
                .thenReturn(List.of(appointment));
        when(delivery.deliverIfDue(21L, 60, LocalDateTime.of(2026, 11, 5, 16, 59),
                LocalDateTime.of(2026, 11, 5, 17, 0), NOW)).thenReturn(NotificationDeliveryResult.SENT);

        assertEquals(1, service.processDueReminders());
        verify(delivery).deliverIfDue(21L, 60, LocalDateTime.of(2026, 11, 5, 16, 59),
                LocalDateTime.of(2026, 11, 5, 17, 0), NOW);
    }

    @Test
    void ignoresAppointmentsOutsideTheDueWindow() {
        Appointment appointment = appointment(22L, LocalDateTime.of(2026, 11, 5, 18, 30));
        when(appointments.findReminderCandidates(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 5),
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)))
                .thenReturn(List.of(appointment));

        assertEquals(0, service.processDueReminders());
        verify(delivery, never()).deliverIfDue(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reminderWindowSupportsAppointmentsAcrossMidnight() {
        LocalDateTime midnightNow = LocalDateTime.of(2026, 11, 5, 23, 59, 30);
        Clock midnightClock = Clock.fixed(Instant.parse("2026-11-05T23:59:30Z"), ZoneOffset.UTC);
        ReminderProcessingService midnightService = new ReminderProcessingService(appointments, delivery,
                new NotificationProperties(true, 60, Duration.ofMinutes(1)), midnightClock);
        Appointment appointment = appointment(23L, LocalDateTime.of(2026, 11, 6, 0, 59));
        when(appointments.findReminderCandidates(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 6),
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)))
                .thenReturn(List.of(appointment));
        when(delivery.deliverIfDue(23L, 60, LocalDateTime.of(2026, 11, 6, 0, 58, 30),
                LocalDateTime.of(2026, 11, 6, 0, 59, 30), midnightNow))
                .thenReturn(NotificationDeliveryResult.SENT);

        assertEquals(1, midnightService.processDueReminders());
        verify(delivery).deliverIfDue(23L, 60, LocalDateTime.of(2026, 11, 6, 0, 58, 30),
                LocalDateTime.of(2026, 11, 6, 0, 59, 30), midnightNow);
    }

    private Appointment appointment(Long id, LocalDateTime start) {
        Owner owner = new Owner("Ana Lopez", "5551234567", "ana@example.com", "Calle 1");
        Pet pet = new Pet("Luna", "Perro", "Labrador", Sex.FEMALE, LocalDate.of(2022, 1, 1),
                BigDecimal.TEN, null, owner);
        Veterinarian veterinarian = new Veterinarian("Dra. Silva", "doctor@example.com", "General");
        Appointment appointment = new Appointment(owner, pet, veterinarian, start.toLocalDate(),
                start.toLocalTime(), start.toLocalTime().plusMinutes(30), "Revision", AppointmentStatus.PENDING);
        ReflectionTestUtils.setField(appointment, "id", id);
        return appointment;
    }
}
