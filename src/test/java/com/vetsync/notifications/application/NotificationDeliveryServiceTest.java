package com.vetsync.notifications.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.model.AppointmentStatus;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.model.Sex;
import com.vetsync.domain.model.Veterinarian;
import com.vetsync.domain.repository.AppointmentRepository;
import com.vetsync.notifications.domain.EmailMessage;
import com.vetsync.notifications.domain.EmailSender;
import com.vetsync.notifications.domain.NotificationHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {
    private static final long APPOINTMENT_ID = 11L;
    private static final long REMINDER_MINUTES = 60L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 5, 16, 0);
    private static final LocalDateTime WINDOW_START = LocalDateTime.of(2026, 11, 5, 16, 59);
    private static final LocalDateTime WINDOW_END = LocalDateTime.of(2026, 11, 5, 17, 0);

    @Mock
    private AppointmentRepository appointments;

    @Mock
    private NotificationHistoryRepository history;

    @Mock
    private EmailSender emailSender;

    private NotificationDeliveryService service;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        service = new NotificationDeliveryService(appointments, history, emailSender);
        Owner owner = new Owner("Ana Lopez", "5551234567", "ana@example.com", "Calle 1");
        Pet pet = new Pet("Luna", "Perro", "Labrador", Sex.FEMALE, LocalDate.of(2022, 1, 1),
                BigDecimal.TEN, null, owner);
        Veterinarian veterinarian = new Veterinarian("Dra. Silva", "doctor@example.com", "General");
        appointment = new Appointment(owner, pet, veterinarian, LocalDate.of(2026, 11, 5),
                LocalTime.of(17, 0), LocalTime.of(17, 30), "Revision", AppointmentStatus.CONFIRMED);
        ReflectionTestUtils.setField(appointment, "id", APPOINTMENT_ID);
        lenient().when(appointments.findByIdForUpdate(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
    }

    @Test
    void sendsAndRecordsAUniqueReminderWithAppointmentDetails() {
        when(history.claim(APPOINTMENT_ID, REMINDER_MINUTES, "ana@example.com", "Luna", "Dra. Silva",
                LocalDateTime.of(2026, 11, 5, 17, 0))).thenReturn(true);

        NotificationDeliveryResult result = service.deliverIfDue(
                APPOINTMENT_ID, REMINDER_MINUTES, WINDOW_START, WINDOW_END, NOW);

        assertEquals(NotificationDeliveryResult.SENT, result);
        ArgumentCaptor<EmailMessage> message = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender).send(message.capture());
        assertEquals("ana@example.com", message.getValue().recipientEmail());
        assertEquals("appointment-11-reminder-60", message.getValue().idempotencyKey());
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertTrue(message.getValue().body().contains("Luna")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(message.getValue().body().contains("2026-11-05")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(message.getValue().body().contains("17:00")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(message.getValue().body().contains("Dra. Silva")));
        verify(history).markSent(APPOINTMENT_ID, REMINDER_MINUTES);
    }

    @Test
    void doesNotSendOrClaimACancelledAppointment() {
        appointment.changeStatus(AppointmentStatus.CANCELLED);

        NotificationDeliveryResult result = service.deliverIfDue(
                APPOINTMENT_ID, REMINDER_MINUTES, WINDOW_START, WINDOW_END, NOW);

        assertEquals(NotificationDeliveryResult.NOT_ELIGIBLE, result);
        verify(history, never()).claim(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        verify(emailSender, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsAClaimedReminderWithoutSendingItAgain() {
        when(history.claim(APPOINTMENT_ID, REMINDER_MINUTES, "ana@example.com", "Luna", "Dra. Silva",
                LocalDateTime.of(2026, 11, 5, 17, 0))).thenReturn(false);

        NotificationDeliveryResult result = service.deliverIfDue(
                APPOINTMENT_ID, REMINDER_MINUTES, WINDOW_START, WINDOW_END, NOW);

        assertEquals(NotificationDeliveryResult.ALREADY_PROCESSED, result);
        verify(emailSender, never()).send(org.mockito.ArgumentMatchers.any());
        verify(history, never()).markSent(APPOINTMENT_ID, REMINDER_MINUTES);
    }

    @Test
    void recordsProviderFailureWithoutLosingItsHistory() {
        when(history.claim(APPOINTMENT_ID, REMINDER_MINUTES, "ana@example.com", "Luna", "Dra. Silva",
                LocalDateTime.of(2026, 11, 5, 17, 0))).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("simulated failure"))
                .when(emailSender).send(org.mockito.ArgumentMatchers.any());

        NotificationDeliveryResult result = service.deliverIfDue(
                APPOINTMENT_ID, REMINDER_MINUTES, WINDOW_START, WINDOW_END, NOW);

        assertEquals(NotificationDeliveryResult.FAILED, result);
        verify(history).markFailed(APPOINTMENT_ID, REMINDER_MINUTES,
                "El envio simulado fallo (IllegalStateException)");
        verify(history, never()).markSent(APPOINTMENT_ID, REMINDER_MINUTES);
    }

    @Test
    void skipsAppointmentWhoseScheduleMovedOutsideTheReminderWindow() {
        appointment.updateSchedule(LocalDate.of(2026, 11, 5), LocalTime.of(18, 30), LocalTime.of(19, 0),
                "Reprogramada", AppointmentStatus.CONFIRMED, appointment.getPet(), appointment.getVeterinarian());

        NotificationDeliveryResult result = service.deliverIfDue(
                APPOINTMENT_ID, REMINDER_MINUTES, WINDOW_START, WINDOW_END, NOW);

        assertEquals(NotificationDeliveryResult.NOT_ELIGIBLE, result);
        verify(history, never()).claim(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        verify(emailSender, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportsAppointmentDeletedBeforeItCanBeProcessed() {
        when(appointments.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.deliverIfDue(
                404L, REMINDER_MINUTES, WINDOW_START, WINDOW_END, NOW));
    }
}
