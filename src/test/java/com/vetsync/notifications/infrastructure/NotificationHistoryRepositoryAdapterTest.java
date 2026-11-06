package com.vetsync.notifications.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NotificationHistoryRepositoryAdapterTest {

    private final SpringDataNotificationHistoryRepository history = mock(SpringDataNotificationHistoryRepository.class);
    private final NotificationHistoryRepositoryAdapter adapter = new NotificationHistoryRepositoryAdapter(history);
    private final LocalDateTime start = LocalDateTime.of(2030, 1, 10, 9, 0);

    @Test
    void claimReturnsTrueWhenNewRowIsInserted() {
        when(history.claim(7L, 60L, "ana@example.com", "Luna", "Dra. Ruiz", start)).thenReturn(1);

        assertThat(adapter.claim(7L, 60L, "ana@example.com", "Luna", "Dra. Ruiz", start)).isTrue();
    }

    @Test
    void claimReturnsFalseWhenReminderWasAlreadyClaimed() {
        when(history.claim(7L, 60L, "ana@example.com", "Luna", "Dra. Ruiz", start)).thenReturn(0);
        when(history.existsByAppointmentIdAndReminderMinutes(7L, 60L)).thenReturn(true);

        assertThat(adapter.claim(7L, 60L, "ana@example.com", "Luna", "Dra. Ruiz", start)).isFalse();
    }

    @Test
    void claimFailsWhenInsertIgnoredAndNoExistingRow() {
        when(history.claim(7L, 60L, "ana@example.com", "Luna", "Dra. Ruiz", start)).thenReturn(0);
        when(history.existsByAppointmentIdAndReminderMinutes(7L, 60L)).thenReturn(false);

        assertThatThrownBy(() -> adapter.claim(7L, 60L, "ana@example.com", "Luna", "Dra. Ruiz", start))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo reservar el registro de notificacion");
    }

    @Test
    void existsDelegatesToSpringData() {
        when(history.existsByAppointmentIdAndReminderMinutes(7L, 60L)).thenReturn(true);

        assertThat(adapter.exists(7L, 60L)).isTrue();
    }

    @Test
    void markSentSucceedsWhenOneRowUpdated() {
        when(history.markSent(7L, 60L)).thenReturn(1);

        adapter.markSent(7L, 60L);
    }

    @Test
    void markSentFailsWhenNoRowUpdated() {
        when(history.markSent(7L, 60L)).thenReturn(0);

        assertThatThrownBy(() -> adapter.markSent(7L, 60L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo actualizar el estado del historial de notificacion");
    }

    @Test
    void markFailedFailsWhenNoRowUpdated() {
        when(history.markFailed(7L, 60L, "timeout")).thenReturn(2);

        assertThatThrownBy(() -> adapter.markFailed(7L, 60L, "timeout"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markFailedSucceedsWhenOneRowUpdated() {
        when(history.markFailed(7L, 60L, "timeout")).thenReturn(1);

        adapter.markFailed(7L, 60L, "timeout");
    }
}
