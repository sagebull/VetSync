package com.vetsync.api.controller;

import com.vetsync.api.dto.AppointmentResponse;
import com.vetsync.api.dto.AppointmentStatusRequest;
import com.vetsync.api.dto.CreateAppointmentRequest;
import com.vetsync.api.dto.UpdateAppointmentRequest;
import com.vetsync.application.service.AppointmentService;
import com.vetsync.domain.model.Appointment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@Validated
@RestController
@RequestMapping("/api")
public class AppointmentController {
    private final AppointmentService appointments;

    public AppointmentController(AppointmentService appointments) {
        this.appointments = appointments;
    }

    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@accessAuthorization.canScheduleForPet(authentication, #request.petId())")
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest request) {
        return toResponse(appointments.create(request.petId(), request.veterinarianId(), request.date(),
                request.startTime(), request.endTime(), request.reason()));
    }

    @GetMapping("/appointments/{id}")
    @PreAuthorize("@accessAuthorization.canAccessAppointment(authentication, #id)")
    public AppointmentResponse get(@PathVariable @Positive Long id) {
        return toResponse(appointments.get(id));
    }

    @PutMapping("/appointments/{id}")
    @PreAuthorize("@accessAuthorization.canRescheduleAppointment(authentication, #id, #request.petId(), #request.status())")
    public AppointmentResponse update(@PathVariable @Positive Long id,
                                      @Valid @RequestBody UpdateAppointmentRequest request) {
        return toResponse(appointments.update(id, request.petId(), request.veterinarianId(), request.date(),
                request.startTime(), request.endTime(), request.reason(), request.status()));
    }

    @PatchMapping("/appointments/{id}/status")
    @PreAuthorize("@accessAuthorization.canChangeAppointmentStatus(authentication, #id, #request.status())")
    public AppointmentResponse changeStatus(@PathVariable @Positive Long id,
                                            @Valid @RequestBody AppointmentStatusRequest request) {
        return toResponse(appointments.changeStatus(id, request.status()));
    }

    @GetMapping("/veterinarians/{veterinarianId}/appointments/day")
    @PreAuthorize("hasRole('STAFF')")
    public List<AppointmentResponse> findByDay(@PathVariable @Positive Long veterinarianId,
                                               @RequestParam LocalDate date) {
        return appointments.findByDay(veterinarianId, date).stream().map(this::toResponse).toList();
    }

    @GetMapping("/veterinarians/{veterinarianId}/appointments/week")
    @PreAuthorize("hasRole('STAFF')")
    public List<AppointmentResponse> findByWeek(@PathVariable @Positive Long veterinarianId,
                                                @RequestParam LocalDate date) {
        return appointments.findByWeek(veterinarianId, date).stream().map(this::toResponse).toList();
    }

    @GetMapping("/veterinarians/{veterinarianId}/appointments/month")
    @PreAuthorize("hasRole('STAFF')")
    public List<AppointmentResponse> findByMonth(@PathVariable @Positive Long veterinarianId,
                                                 @RequestParam @Min(1) @Max(9999) int year,
                                                 @RequestParam @Min(1) @Max(12) int month) {
        return appointments.findByMonth(veterinarianId, year, month).stream().map(this::toResponse).toList();
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(appointment.getId(), appointment.getOwner().getId(),
                appointment.getPet().getId(), appointment.getVeterinarian().getId(),
                appointment.getAppointmentDate(), appointment.getStartTime(), appointment.getEndTime(),
                appointment.getReason(), appointment.getStatus());
    }
}
