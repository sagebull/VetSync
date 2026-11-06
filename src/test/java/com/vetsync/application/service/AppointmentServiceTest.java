package com.vetsync.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.application.exception.ResourceConflictException;
import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.model.AppointmentStatus;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.model.Sex;
import com.vetsync.domain.model.Veterinarian;
import com.vetsync.domain.repository.AppointmentRepository;
import com.vetsync.domain.repository.PetRepository;
import com.vetsync.domain.repository.VeterinarianRepository;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {
    private static final Long PET_ID = 2L;
    private static final Long VETERINARIAN_ID = 4L;
    private static final LocalDate FUTURE_DATE = LocalDate.of(2026, 11, 6);
    private static final LocalTime START = LocalTime.of(9, 0);
    private static final LocalTime END = LocalTime.of(9, 30);

    @Mock
    private AppointmentRepository appointments;

    @Mock
    private PetRepository pets;

    @Mock
    private VeterinarianRepository veterinarians;

    private AppointmentService service;
    private Owner owner;
    private Pet pet;
    private Veterinarian veterinarian;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-05T16:00:00Z"), ZoneOffset.UTC);
        service = new AppointmentService(appointments, pets, veterinarians, clock);
        owner = new Owner("Ana Lopez", "5551234567", "ana@example.com", "Calle 1");
        pet = new Pet("Luna", "Perro", "Labrador", Sex.FEMALE, LocalDate.of(2022, 1, 1),
                BigDecimal.TEN, null, owner);
        veterinarian = new Veterinarian("Dr. Silva", "doctor@example.com", "General");
        ReflectionTestUtils.setField(veterinarian, "id", VETERINARIAN_ID);
    }

    @Test
    void createSchedulesPendingAppointmentAfterLockingVeterinarian() {
        when(veterinarians.findByIdForUpdate(VETERINARIAN_ID)).thenReturn(Optional.of(veterinarian));
        when(pets.findById(PET_ID)).thenReturn(Optional.of(pet));
        when(appointments.existsOverlapping(VETERINARIAN_ID, FUTURE_DATE, START, END, null)).thenReturn(false);
        when(appointments.save(org.mockito.ArgumentMatchers.any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = service.create(PET_ID, VETERINARIAN_ID, FUTURE_DATE, START, END, "Revision");

        assertEquals(owner, result.getOwner());
        assertEquals(pet, result.getPet());
        assertEquals(veterinarian, result.getVeterinarian());
        assertEquals(AppointmentStatus.PENDING, result.getStatus());
        InOrder bookingOrder = inOrder(veterinarians, appointments);
        bookingOrder.verify(veterinarians).findByIdForUpdate(VETERINARIAN_ID);
        bookingOrder.verify(appointments).existsOverlapping(VETERINARIAN_ID, FUTURE_DATE, START, END, null);
    }

    @Test
    void createRejectsOverlapAndDoesNotSave() {
        when(veterinarians.findByIdForUpdate(VETERINARIAN_ID)).thenReturn(Optional.of(veterinarian));
        when(pets.findById(PET_ID)).thenReturn(Optional.of(pet));
        when(appointments.existsOverlapping(VETERINARIAN_ID, FUTURE_DATE, START, END, null)).thenReturn(true);

        assertThrows(ResourceConflictException.class,
                () -> service.create(PET_ID, VETERINARIAN_ID, FUTURE_DATE, START, END, "Revision"));

        verify(appointments, never()).save(org.mockito.ArgumentMatchers.any(Appointment.class));
    }

    @Test
    void createAcceptsAdjacentTimeSlot() {
        LocalTime adjacentStart = END;
        LocalTime adjacentEnd = LocalTime.of(10, 0);
        when(veterinarians.findByIdForUpdate(VETERINARIAN_ID)).thenReturn(Optional.of(veterinarian));
        when(pets.findById(PET_ID)).thenReturn(Optional.of(pet));
        when(appointments.existsOverlapping(VETERINARIAN_ID, FUTURE_DATE,
                adjacentStart, adjacentEnd, null)).thenReturn(false);
        when(appointments.save(org.mockito.ArgumentMatchers.any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = service.create(PET_ID, VETERINARIAN_ID, FUTURE_DATE,
                adjacentStart, adjacentEnd, "Siguiente consulta");

        assertEquals(adjacentStart, result.getStartTime());
    }

    @Test
    void createRejectsPastAppointment() {
        LocalDate pastDate = LocalDate.of(2026, 11, 4);
        when(veterinarians.findByIdForUpdate(VETERINARIAN_ID)).thenReturn(Optional.of(veterinarian));
        when(pets.findById(PET_ID)).thenReturn(Optional.of(pet));

        assertThrows(IllegalArgumentException.class, () -> service.create(PET_ID, VETERINARIAN_ID,
                pastDate, START, END, "Revision"));

        verify(appointments, never()).existsOverlapping(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createRejectsInvalidTimeRange() {
        when(veterinarians.findByIdForUpdate(VETERINARIAN_ID)).thenReturn(Optional.of(veterinarian));
        when(pets.findById(PET_ID)).thenReturn(Optional.of(pet));

        assertThrows(IllegalArgumentException.class, () -> service.create(PET_ID, VETERINARIAN_ID,
                FUTURE_DATE, END, START, "Revision"));
    }

    @Test
    void createRejectsUnknownVeterinarianBeforeLookingUpPet() {
        when(veterinarians.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(PET_ID, 404L,
                FUTURE_DATE, START, END, "Revision"));

        verify(pets, never()).findById(PET_ID);
    }

    @Test
    void createRejectsUnknownPet() {
        when(veterinarians.findByIdForUpdate(VETERINARIAN_ID)).thenReturn(Optional.of(veterinarian));
        when(pets.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(404L, VETERINARIAN_ID,
                FUTURE_DATE, START, END, "Revision"));
    }

    @Test
    void cancellationDoesNotCheckOrReserveTheTimeSlot() {
        Appointment appointment = appointment(AppointmentStatus.CONFIRMED, FUTURE_DATE, START, END);
        when(appointments.findVeterinarianIdById(15L)).thenReturn(Optional.of(VETERINARIAN_ID));
        when(veterinarians.findByIdForUpdate(VETERINARIAN_ID)).thenReturn(Optional.of(veterinarian));
        when(appointments.findByIdForUpdate(15L)).thenReturn(Optional.of(appointment));
        when(appointments.save(appointment)).thenReturn(appointment);

        Appointment result = service.changeStatus(15L, AppointmentStatus.CANCELLED);

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        verify(appointments, never()).existsOverlapping(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reactivatingCancelledAppointmentChecksAvailability() {
        Appointment appointment = appointment(AppointmentStatus.CANCELLED, FUTURE_DATE, START, END);
        when(appointments.findVeterinarianIdById(15L)).thenReturn(Optional.of(VETERINARIAN_ID));
        when(veterinarians.findByIdForUpdate(VETERINARIAN_ID)).thenReturn(Optional.of(veterinarian));
        when(appointments.findByIdForUpdate(15L)).thenReturn(Optional.of(appointment));
        when(appointments.existsOverlapping(VETERINARIAN_ID, FUTURE_DATE, START, END, 15L)).thenReturn(true);

        assertThrows(ResourceConflictException.class,
                () -> service.changeStatus(15L, AppointmentStatus.CONFIRMED));

        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        verify(appointments, never()).save(appointment);
    }

    @Test
    void weekQueryUsesMondayToFollowingMonday() {
        LocalDate requestedDate = LocalDate.of(2026, 11, 5);
        when(veterinarians.findById(VETERINARIAN_ID)).thenReturn(Optional.of(veterinarian));
        when(appointments.findByVeterinarianAndDateRange(VETERINARIAN_ID,
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9))).thenReturn(java.util.List.of());

        service.findByWeek(VETERINARIAN_ID, requestedDate);

        verify(appointments).findByVeterinarianAndDateRange(
                VETERINARIAN_ID, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9));
    }

    @Test
    void monthQueryUsesFirstDayToFirstDayOfNextMonth() {
        when(veterinarians.findById(VETERINARIAN_ID)).thenReturn(Optional.of(veterinarian));
        when(appointments.findByVeterinarianAndDateRange(VETERINARIAN_ID,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1))).thenReturn(java.util.List.of());

        service.findByMonth(VETERINARIAN_ID, 2026, 11);

        verify(appointments).findByVeterinarianAndDateRange(
                VETERINARIAN_ID, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1));
    }

    @Test
    void monthQueryRejectsInvalidMonth() {
        when(veterinarians.findById(VETERINARIAN_ID)).thenReturn(Optional.of(veterinarian));

        assertThrows(IllegalArgumentException.class, () -> service.findByMonth(VETERINARIAN_ID, 2026, 13));
    }

    private Appointment appointment(AppointmentStatus status, LocalDate date,
                                    LocalTime startTime, LocalTime endTime) {
        Appointment appointment = new Appointment(owner, pet, veterinarian, date, startTime, endTime,
                "Revision", status);
        ReflectionTestUtils.setField(appointment, "id", 15L);
        return appointment;
    }
}
