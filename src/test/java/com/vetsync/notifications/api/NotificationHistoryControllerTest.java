package com.vetsync.notifications.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.api.exception.ApiExceptionHandler;
import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.notifications.application.NotificationHistoryService;
import com.vetsync.notifications.domain.NotificationDeliveryStatus;
import com.vetsync.notifications.domain.NotificationHistoryEntry;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificationHistoryControllerTest {

    private final NotificationHistoryService history = mock(NotificationHistoryService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationHistoryController(history))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void findByAppointmentReturnsHistoryEntriesAsResponses() throws Exception {
        when(history.findByAppointment(7L)).thenReturn(List.of(new NotificationHistoryEntry(
                1L, 7L, 1440L, "ana@example.com", "Luna", "Dra. Lopez",
                LocalDateTime.of(2030, 1, 10, 9, 0), NotificationDeliveryStatus.SENT,
                LocalDateTime.of(2030, 1, 9, 9, 0), LocalDateTime.of(2030, 1, 9, 9, 1), null)));

        mockMvc.perform(get("/api/appointments/7/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].reminderMinutes").value(1440))
                .andExpect(jsonPath("$[0].status").value("SENT"))
                .andExpect(jsonPath("$[0].recipientEmail").value("ana@example.com"));
    }

    @Test
    void findByAppointmentReturnsNotFoundForUnknownAppointment() throws Exception {
        when(history.findByAppointment(99L)).thenThrow(new ResourceNotFoundException("Cita", 99L));

        mockMvc.perform(get("/api/appointments/99/notifications"))
                .andExpect(status().isNotFound());
    }
}
