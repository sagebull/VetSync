package com.vetsync.notifications.application;

import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.repository.AppointmentRepository;
import com.vetsync.notifications.domain.NotificationHistoryEntry;
import com.vetsync.notifications.domain.NotificationHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationHistoryService {
    private final AppointmentRepository appointments;
    private final NotificationHistoryRepository history;

    public NotificationHistoryService(AppointmentRepository appointments, NotificationHistoryRepository history) {
        this.appointments = appointments;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public List<NotificationHistoryEntry> findByAppointment(Long appointmentId) {
        if (appointments.findById(appointmentId).isEmpty()) {
            throw new ResourceNotFoundException("Cita", appointmentId);
        }
        return history.findByAppointmentId(appointmentId);
    }
}
