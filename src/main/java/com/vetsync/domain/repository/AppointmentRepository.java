package com.vetsync.domain.repository;

import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.model.AppointmentStatus;
import java.util.Collection;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {
    Optional<Appointment> findById(Long id);

    boolean existsByIdAndOwnerId(Long appointmentId, Long ownerId);

    Optional<AppointmentStatus> findStatusByIdAndOwnerId(Long appointmentId, Long ownerId);

    Optional<Appointment> findByIdForUpdate(Long id);

    List<Appointment> findReminderCandidates(LocalDate startDate, LocalDate endDate,
                                            Collection<AppointmentStatus> statuses);

    Optional<Long> findVeterinarianIdById(Long id);

    boolean existsOverlapping(Long veterinarianId, LocalDate date, LocalTime startTime, LocalTime endTime,
                              Long excludedAppointmentId);

    List<Appointment> findByVeterinarianAndDateRange(Long veterinarianId, LocalDate startDate, LocalDate endDate);

    Appointment save(Appointment appointment);
}
