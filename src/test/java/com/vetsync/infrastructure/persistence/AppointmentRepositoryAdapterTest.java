package com.vetsync.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsync.domain.model.Appointment;
import com.vetsync.domain.model.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AppointmentRepositoryAdapterTest {

    private final SpringDataAppointmentRepository appointments = mock(SpringDataAppointmentRepository.class);
    private final AppointmentRepositoryAdapter adapter = new AppointmentRepositoryAdapter(appointments);

    @Test
    void findByIdReturnsAppointmentOrEmpty() {
        Appointment appointment = mock(Appointment.class);
        when(appointments.findById(7L)).thenReturn(Optional.of(appointment));
        when(appointments.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(7L)).containsSame(appointment);
        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void existsByIdAndOwnerIdUsesOwnerScopedQuery() {
        when(appointments.existsByIdAndOwner_Id(7L, 5L)).thenReturn(true);

        assertThat(adapter.existsByIdAndOwnerId(7L, 5L)).isTrue();
    }

    @Test
    void findStatusByIdAndOwnerIdReturnsStatusOrEmpty() {
        when(appointments.findStatusByIdAndOwner_Id(7L, 5L)).thenReturn(Optional.of(AppointmentStatus.CONFIRMED));
        when(appointments.findStatusByIdAndOwner_Id(7L, 6L)).thenReturn(Optional.empty());

        assertThat(adapter.findStatusByIdAndOwnerId(7L, 5L)).contains(AppointmentStatus.CONFIRMED);
        assertThat(adapter.findStatusByIdAndOwnerId(7L, 6L)).isEmpty();
    }

    @Test
    void findByIdForUpdateReturnsLockedAppointmentOrEmpty() {
        Appointment appointment = mock(Appointment.class);
        when(appointments.findByIdForUpdate(7L)).thenReturn(Optional.of(appointment));

        assertThat(adapter.findByIdForUpdate(7L)).containsSame(appointment);
    }

    @Test
    void existsOverlappingExcludesCancelledAppointments() {
        LocalDate date = LocalDate.of(2030, 1, 10);
        when(appointments.existsOverlapping(3L, date, LocalTime.of(9, 0), LocalTime.of(9, 30),
                AppointmentStatus.CANCELLED, 7L)).thenReturn(true);

        assertThat(adapter.existsOverlapping(3L, date, LocalTime.of(9, 0), LocalTime.of(9, 30), 7L)).isTrue();
    }

    @Test
    void findByVeterinarianAndDateRangeDelegatesToDerivedQuery() {
        List<Appointment> found = List.of(mock(Appointment.class));
        LocalDate start = LocalDate.of(2030, 1, 1);
        LocalDate end = LocalDate.of(2030, 2, 1);
        when(appointments
                .findByVeterinarian_IdAndAppointmentDateGreaterThanEqualAndAppointmentDateLessThanOrderByAppointmentDateAscStartTimeAsc(
                        3L, start, end)).thenReturn(found);

        assertThat(adapter.findByVeterinarianAndDateRange(3L, start, end)).isSameAs(found);
    }

    @Test
    void saveReturnsPersistedAppointment() {
        Appointment appointment = mock(Appointment.class);
        when(appointments.save(appointment)).thenReturn(appointment);

        assertThat(adapter.save(appointment)).isSameAs(appointment);
    }

    @Test
    void findVeterinarianIdByIdReturnsIdOrEmpty() {
        when(appointments.findVeterinarianIdById(7L)).thenReturn(Optional.of(3L));
        when(appointments.findVeterinarianIdById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findVeterinarianIdById(7L)).contains(3L);
        assertThat(adapter.findVeterinarianIdById(99L)).isEmpty();
    }

    @Test
    void findReminderCandidatesPassesRangeAndStatusesThrough() {
        LocalDate start = LocalDate.of(2030, 1, 10);
        LocalDate end = LocalDate.of(2030, 1, 11);
        List<AppointmentStatus> statuses = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
        List<Appointment> found = List.of(mock(Appointment.class));
        when(appointments.findReminderCandidates(start, end, statuses)).thenReturn(found);

        assertThat(adapter.findReminderCandidates(start, end, statuses)).isSameAs(found);
    }
}
