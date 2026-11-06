package com.vetsync.notifications.application;

import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.model.AppointmentStatus;
import com.vetsync.domain.repository.AppointmentRepository;
import com.vetsync.notifications.domain.EmailMessage;
import com.vetsync.notifications.domain.EmailSender;
import com.vetsync.notifications.domain.NotificationHistoryRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDeliveryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDeliveryService.class);

    private final AppointmentRepository appointments;
    private final NotificationHistoryRepository history;
    private final EmailSender emailSender;

    public NotificationDeliveryService(AppointmentRepository appointments,
                                       NotificationHistoryRepository history,
                                       EmailSender emailSender) {
        this.appointments = appointments;
        this.history = history;
        this.emailSender = emailSender;
    }

    @Transactional
    public NotificationDeliveryResult deliverIfDue(Long appointmentId, long reminderMinutes,
                                                    LocalDateTime dueWindowStart,
                                                    LocalDateTime dueWindowEnd,
                                                    LocalDateTime now) {
        Appointment appointment = appointments.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new IllegalStateException(
                        "La cita desaparecio mientras se procesaba el recordatorio"));

        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            return NotificationDeliveryResult.NOT_ELIGIBLE;
        }

        LocalDateTime appointmentStart = LocalDateTime.of(
                appointment.getAppointmentDate(), appointment.getStartTime());
        if (!appointmentStart.isAfter(now)
                || appointmentStart.isBefore(dueWindowStart)
                || appointmentStart.isAfter(dueWindowEnd)) {
            return NotificationDeliveryResult.NOT_ELIGIBLE;
        }

        if (!history.claim(appointmentId, reminderMinutes, appointment.getOwner().getEmail(),
                appointment.getPet().getName(), appointment.getVeterinarian().getFullName(), appointmentStart)) {
            return NotificationDeliveryResult.ALREADY_PROCESSED;
        }

        String idempotencyKey = "appointment-" + appointmentId + "-reminder-" + reminderMinutes;
        EmailMessage message = new EmailMessage(idempotencyKey, appointment.getOwner().getEmail(),
                "Recordatorio de cita veterinaria",
                "Hola, le recordamos la cita de " + appointment.getPet().getName()
                        + " el " + appointmentStart.toLocalDate()
                        + " a las " + appointmentStart.toLocalTime()
                        + " con " + appointment.getVeterinarian().getFullName() + ".");
        try {
            emailSender.send(message);
        } catch (RuntimeException exception) {
            String errorMessage = "El envio simulado fallo (" + exception.getClass().getSimpleName() + ")";
            history.markFailed(appointmentId, reminderMinutes, errorMessage);
            LOGGER.error("Fallo el procesamiento de la notificacion para la cita {} y anticipacion de {} minutos",
                    appointmentId, reminderMinutes, exception);
            return NotificationDeliveryResult.FAILED;
        }

        history.markSent(appointmentId, reminderMinutes);
        LOGGER.info("Notificacion registrada para la cita {} y anticipacion de {} minutos",
                appointmentId, reminderMinutes);
        return NotificationDeliveryResult.SENT;
    }
}
