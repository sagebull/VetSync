package com.vetsync.application.service;

import com.vetsync.application.exception.ResourceConflictException;
import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.model.AppointmentStatus;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.model.Veterinarian;
import com.vetsync.domain.repository.AppointmentRepository;
import com.vetsync.domain.repository.PetRepository;
import com.vetsync.domain.repository.VeterinarianRepository;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final PetRepository pets;
    private final VeterinarianRepository veterinarians;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointments, PetRepository pets,
                              VeterinarianRepository veterinarians, Clock clock) {
        this.appointments = appointments;
        this.pets = pets;
        this.veterinarians = veterinarians;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment create(Long petId, Long veterinarianId, LocalDate date, LocalTime startTime,
                              LocalTime endTime, String reason) {
        Veterinarian veterinarian = lockVeterinarian(veterinarianId);
        Pet pet = findPet(petId);
        validateSchedule(date, startTime, endTime, true);
        ensureAvailable(veterinarianId, date, startTime, endTime, null);
        return appointments.save(new Appointment(pet.getOwner(), pet, veterinarian, date, startTime, endTime,
                reason, AppointmentStatus.PENDING));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment update(Long id, Long petId, Long veterinarianId, LocalDate date, LocalTime startTime,
                              LocalTime endTime, String reason, AppointmentStatus status) {
        Veterinarian veterinarian = lockVeterinarian(veterinarianId);
        Appointment appointment = appointments.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", id));
        Pet pet = findPet(petId);
        boolean scheduleChanged = !appointment.getPet().getId().equals(petId)
                || !appointment.getVeterinarian().getId().equals(veterinarianId)
                || !appointment.getAppointmentDate().equals(date)
                || !appointment.getStartTime().equals(startTime)
                || !appointment.getEndTime().equals(endTime);
        validateSchedule(date, startTime, endTime, scheduleChanged);
        validateStatusTime(status, date, startTime, endTime);
        if (status != AppointmentStatus.CANCELLED) {
            ensureAvailable(veterinarianId, date, startTime, endTime, id);
        }
        appointment.updateSchedule(date, startTime, endTime, reason, status, pet, veterinarian);
        return appointments.save(appointment);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Appointment changeStatus(Long id, AppointmentStatus status) {
        Long veterinarianId = appointments.findVeterinarianIdById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", id));
        lockVeterinarian(veterinarianId);
        Appointment appointment = appointments.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", id));
        if (!appointment.getVeterinarian().getId().equals(veterinarianId)) {
            throw new ResourceConflictException("La cita fue modificada concurrentemente; consulta su estado e intenta nuevamente");
        }
        validateStatusTime(status, appointment.getAppointmentDate(),
                appointment.getStartTime(), appointment.getEndTime());
        if (status != AppointmentStatus.CANCELLED) {
            ensureAvailable(veterinarianId, appointment.getAppointmentDate(), appointment.getStartTime(),
                    appointment.getEndTime(), id);
        }
        appointment.changeStatus(status);
        return appointments.save(appointment);
    }

    @Transactional(readOnly = true)
    public Appointment get(Long id) {
        return appointments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cita", id));
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByDay(Long veterinarianId, LocalDate date) {
        requireVeterinarian(veterinarianId);
        try {
            return appointments.findByVeterinarianAndDateRange(veterinarianId, date, date.plusDays(1));
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("La fecha solicitada esta fuera del rango admitido", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByWeek(Long veterinarianId, LocalDate date) {
        requireVeterinarian(veterinarianId);
        try {
            LocalDate startOfWeek = date.minusDays(date.getDayOfWeek().getValue() - 1L);
            return appointments.findByVeterinarianAndDateRange(veterinarianId, startOfWeek,
                    startOfWeek.plusWeeks(1));
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("La semana solicitada esta fuera del rango admitido", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByMonth(Long veterinarianId, int year, int month) {
        requireVeterinarian(veterinarianId);
        YearMonth requestedMonth;
        try {
            requestedMonth = YearMonth.of(year, month);
            LocalDate startOfMonth = requestedMonth.atDay(1);
            LocalDate startOfNextMonth = requestedMonth.plusMonths(1).atDay(1);
            return appointments.findByVeterinarianAndDateRange(veterinarianId, startOfMonth, startOfNextMonth);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("El anio o mes solicitado no es valido", exception);
        }
    }

    private Veterinarian lockVeterinarian(Long veterinarianId) {
        return veterinarians.findByIdForUpdate(veterinarianId)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario", veterinarianId));
    }

    private Veterinarian requireVeterinarian(Long veterinarianId) {
        return veterinarians.findById(veterinarianId)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario", veterinarianId));
    }

    private Pet findPet(Long petId) {
        return pets.findById(petId).orElseThrow(() -> new ResourceNotFoundException("Mascota", petId));
    }

    private void validateSchedule(LocalDate date, LocalTime startTime, LocalTime endTime, boolean requireFuture) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de finalizacion");
        }
        if (requireFuture && !LocalDateTime.of(date, startTime).isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("No se pueden agendar ni reprogramar citas en fechas u horas pasadas");
        }
    }

    private void validateStatusTime(AppointmentStatus status, LocalDate date,
                                    LocalTime startTime, LocalTime endTime) {
        LocalDateTime now = LocalDateTime.now(clock);
        if ((status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED)
                && !LocalDateTime.of(date, startTime).isAfter(now)) {
            throw new IllegalArgumentException("Una cita pendiente o confirmada debe tener un horario futuro");
        }
        if (status == AppointmentStatus.COMPLETED
                && LocalDateTime.of(date, endTime).isAfter(now)) {
            throw new IllegalArgumentException("Solo se pueden completar citas cuya hora de finalizacion ya paso");
        }
    }

    private void ensureAvailable(Long veterinarianId, LocalDate date, LocalTime startTime,
                                 LocalTime endTime, Long excludedAppointmentId) {
        if (appointments.existsOverlapping(veterinarianId, date, startTime, endTime, excludedAppointmentId)) {
            throw new ResourceConflictException("El veterinario ya tiene una cita que se superpone con ese horario");
        }
    }
}
