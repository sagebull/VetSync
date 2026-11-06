package com.vetsync.api.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.api.exception.ApiExceptionHandler;
import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.application.service.AppointmentService;
import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.model.AppointmentStatus;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.model.Veterinarian;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AppointmentControllerTest {

    private final AppointmentService appointments = mock(AppointmentService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AppointmentController(appointments))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    private static Appointment appointment(long id, AppointmentStatus status) {
        Owner owner = mock(Owner.class);
        when(owner.getId()).thenReturn(5L);
        Pet pet = mock(Pet.class);
        when(pet.getId()).thenReturn(10L);
        Veterinarian vet = mock(Veterinarian.class);
        when(vet.getId()).thenReturn(3L);
        Appointment appointment = mock(Appointment.class);
        when(appointment.getId()).thenReturn(id);
        when(appointment.getOwner()).thenReturn(owner);
        when(appointment.getPet()).thenReturn(pet);
        when(appointment.getVeterinarian()).thenReturn(vet);
        when(appointment.getAppointmentDate()).thenReturn(LocalDate.of(2030, 1, 10));
        when(appointment.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(appointment.getEndTime()).thenReturn(LocalTime.of(9, 30));
        when(appointment.getReason()).thenReturn("Vacuna");
        when(appointment.getStatus()).thenReturn(status);
        return appointment;
    }

    @Test
    void createReturnsCreatedAppointmentMappedToResponse() throws Exception {
        Appointment expected = appointment(7L, AppointmentStatus.PENDING);
        when(appointments.create(10L, 3L, LocalDate.of(2030, 1, 10), LocalTime.of(9, 0),
                LocalTime.of(9, 30), "Vacuna")).thenReturn(expected);

        mockMvc.perform(post("/api/appointments").contentType("application/json").content("""
                        {"petId":10,"veterinarianId":3,"date":"2030-01-10","startTime":"09:00",
                         "endTime":"09:30","reason":"Vacuna"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.ownerId").value(5))
                .andExpect(jsonPath("$.petId").value(10))
                .andExpect(jsonPath("$.veterinarianId").value(3))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createRejectsMissingFieldsWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/appointments").contentType("application/json").content("""
                        {"petId":10}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appointments);
    }

    @Test
    void getReturnsAppointmentById() throws Exception {
        Appointment expected = appointment(7L, AppointmentStatus.CONFIRMED);
        when(appointments.get(7L)).thenReturn(expected);

        mockMvc.perform(get("/api/appointments/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Vacuna"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getReturnsNotFoundWhenAppointmentDoesNotExist() throws Exception {
        when(appointments.get(99L)).thenThrow(new ResourceNotFoundException("Cita", 99L));

        mockMvc.perform(get("/api/appointments/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDelegatesToServiceAndReturnsUpdatedAppointment() throws Exception {
        Appointment expected = appointment(7L, AppointmentStatus.CONFIRMED);
        when(appointments.update(eq(7L), eq(10L), eq(3L), eq(LocalDate.of(2030, 2, 1)), eq(LocalTime.of(10, 0)),
                eq(LocalTime.of(10, 30)), eq("Control"), eq(AppointmentStatus.CONFIRMED)))
                .thenReturn(expected);

        mockMvc.perform(put("/api/appointments/7").contentType("application/json").content("""
                        {"petId":10,"veterinarianId":3,"date":"2030-02-01","startTime":"10:00",
                         "endTime":"10:30","reason":"Control","status":"CONFIRMED"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void changeStatusDelegatesStatusToService() throws Exception {
        Appointment expected = appointment(7L, AppointmentStatus.CANCELLED);
        when(appointments.changeStatus(7L, AppointmentStatus.CANCELLED))
                .thenReturn(expected);

        mockMvc.perform(patch("/api/appointments/7/status").contentType("application/json")
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void findByDayReturnsAppointmentsForVeterinarianAndDate() throws Exception {
        List<Appointment> expected = List.of(appointment(7L, AppointmentStatus.PENDING));
        when(appointments.findByDay(3L, LocalDate.of(2030, 1, 10)))
                .thenReturn(expected);

        mockMvc.perform(get("/api/veterinarians/3/appointments/day").param("date", "2030-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));
    }

    @Test
    void findByWeekReturnsAppointmentsForWeekContainingDate() throws Exception {
        List<Appointment> expected = List.of(appointment(8L, AppointmentStatus.PENDING));
        when(appointments.findByWeek(3L, LocalDate.of(2030, 1, 10)))
                .thenReturn(expected);

        mockMvc.perform(get("/api/veterinarians/3/appointments/week").param("date", "2030-01-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8));
    }

    @Test
    void findByMonthReturnsAppointmentsForYearAndMonth() throws Exception {
        List<Appointment> expected = List.of(appointment(9L, AppointmentStatus.COMPLETED));
        when(appointments.findByMonth(3L, 2030, 1))
                .thenReturn(expected);

        mockMvc.perform(get("/api/veterinarians/3/appointments/month").param("year", "2030").param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }
}
