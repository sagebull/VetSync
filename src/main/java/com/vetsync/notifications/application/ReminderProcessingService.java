package com.vetsync.notifications.application;

import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.model.AppointmentStatus;
import com.vetsync.domain.repository.AppointmentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReminderProcessingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReminderProcessingService.class);
    private static final List<AppointmentStatus> REMINDABLE_STATUSES =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointments;
    private final NotificationDeliveryService delivery;
    private final NotificationProperties properties;
    private final Clock clock;

    public ReminderProcessingService(AppointmentRepository appointments, NotificationDeliveryService delivery,
                                     NotificationProperties properties, Clock clock) {
        this.appointments = appointments;
        this.delivery = delivery;
        this.properties = properties;
        this.clock = clock;
    }

    public int processDueReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime windowStart = now.minus(properties.fixedDelay())
                .plusMinutes(properties.reminderMinutes());
        LocalDateTime windowEnd = now.plusMinutes(properties.reminderMinutes());

        List<Appointment> candidates = appointments.findReminderCandidates(
                windowStart.toLocalDate(), windowEnd.toLocalDate(), REMINDABLE_STATUSES);
        int sentCount = 0;
        for (Appointment appointment : candidates) {
            LocalDateTime appointmentStart = LocalDateTime.of(
                    appointment.getAppointmentDate(), appointment.getStartTime());
            if (appointmentStart.isBefore(windowStart) || appointmentStart.isAfter(windowEnd)) {
                continue;
            }

            NotificationDeliveryResult result = delivery.deliverIfDue(
                    appointment.getId(), properties.reminderMinutes(), windowStart, windowEnd, now);
            if (result == NotificationDeliveryResult.SENT) {
                sentCount++;
            }
        }

        if (!candidates.isEmpty()) {
            LOGGER.info("Procesamiento de recordatorios finalizado; candidatos={}, enviados={}",
                    candidates.size(), sentCount);
        }
        return sentCount;
    }
}
