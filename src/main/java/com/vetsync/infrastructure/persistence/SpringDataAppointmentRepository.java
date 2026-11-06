package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.model.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.LockModeType;

public interface SpringDataAppointmentRepository extends JpaRepository<Appointment, Long> {
    boolean existsByIdAndOwner_Id(Long appointmentId, Long ownerId);

    Optional<AppointmentStatus> findStatusByIdAndOwner_Id(Long appointmentId, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Long id);

    @Query("select a.veterinarian.id from Appointment a where a.id = :id")
    Optional<Long> findVeterinarianIdById(@Param("id") Long id);

    @Query("""
            select case when count(a) > 0 then true else false end
            from Appointment a
            where a.veterinarian.id = :veterinarianId
              and a.appointmentDate = :date
              and a.startTime < :endTime
              and a.endTime > :startTime
              and a.status <> :cancelled
              and (:excludedId is null or a.id <> :excludedId)
            """)
    boolean existsOverlapping(@Param("veterinarianId") Long veterinarianId,
                              @Param("date") LocalDate date,
                              @Param("startTime") LocalTime startTime,
                              @Param("endTime") LocalTime endTime,
                              @Param("cancelled") AppointmentStatus cancelled,
                              @Param("excludedId") Long excludedId);

    List<Appointment> findByVeterinarian_IdAndAppointmentDateGreaterThanEqualAndAppointmentDateLessThanOrderByAppointmentDateAscStartTimeAsc(
            Long veterinarianId, LocalDate startDate, LocalDate endDate);

    @Transactional(readOnly = true)
    @Query("""
            select a from Appointment a
            join fetch a.owner
            join fetch a.pet
            join fetch a.veterinarian
            where a.appointmentDate between :startDate and :endDate
              and a.status in :statuses
            order by a.appointmentDate, a.startTime
            """)
    List<Appointment> findReminderCandidates(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("statuses") Collection<AppointmentStatus> statuses);
}
