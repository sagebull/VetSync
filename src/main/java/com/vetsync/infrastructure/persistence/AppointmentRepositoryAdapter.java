package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.model.AppointmentStatus;
import com.vetsync.domain.repository.AppointmentRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AppointmentRepositoryAdapter implements AppointmentRepository {
    private final SpringDataAppointmentRepository appointments;

    public AppointmentRepositoryAdapter(SpringDataAppointmentRepository appointments) {
        this.appointments = appointments;
    }

    @Override
    public Optional<Appointment> findById(Long id) {
        return appointments.findById(id);
    }

    @Override
    public boolean existsByIdAndOwnerId(Long appointmentId, Long ownerId) {
        return appointments.existsByIdAndOwner_Id(appointmentId, ownerId);
    }

    @Override
    public Optional<AppointmentStatus> findStatusByIdAndOwnerId(Long appointmentId, Long ownerId) {
        return appointments.findStatusByIdAndOwner_Id(appointmentId, ownerId);
    }

    @Override
    public Optional<Appointment> findByIdForUpdate(Long id) {
        return appointments.findByIdForUpdate(id);
    }

    @Override
    public List<Appointment> findReminderCandidates(LocalDate startDate, LocalDate endDate,
                                                    Collection<AppointmentStatus> statuses) {
        return appointments.findReminderCandidates(startDate, endDate, statuses);
    }

    @Override
    public Optional<Long> findVeterinarianIdById(Long id) {
        return appointments.findVeterinarianIdById(id);
    }

    @Override
    public boolean existsOverlapping(Long veterinarianId, LocalDate date, LocalTime startTime, LocalTime endTime,
                                     Long excludedAppointmentId) {
        return appointments.existsOverlapping(veterinarianId, date, startTime, endTime,
                AppointmentStatus.CANCELLED, excludedAppointmentId);
    }

    @Override
    public List<Appointment> findByVeterinarianAndDateRange(Long veterinarianId, LocalDate startDate,
                                                            LocalDate endDate) {
        return appointments
                .findByVeterinarian_IdAndAppointmentDateGreaterThanEqualAndAppointmentDateLessThanOrderByAppointmentDateAscStartTimeAsc(
                        veterinarianId, startDate, endDate);
    }

    @Override
    public Appointment save(Appointment appointment) {
        return appointments.save(appointment);
    }
}
