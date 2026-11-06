package com.vetsync.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.api.exception.ApiExceptionHandler;
import com.vetsync.application.service.OwnerRegistrationService;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.OwnerRegistration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OwnerRegistrationAdminControllerTest {

    private final OwnerRegistrationService registrations = mock(OwnerRegistrationService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OwnerRegistrationAdminController(registrations))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void findPendingReturnsPendingRequestsWithoutPasswordHash() throws Exception {
        OwnerRegistration request = mock(OwnerRegistration.class);
        when(request.getId()).thenReturn(8L);
        when(request.getFullName()).thenReturn("Luis Gomez");
        when(request.getPhone()).thenReturn("555-0200");
        when(request.getEmail()).thenReturn("luis@example.com");
        when(request.getAddress()).thenReturn("Avenida 2");
        when(request.getCreatedAt()).thenReturn(LocalDateTime.of(2030, 1, 1, 9, 0));
        when(registrations.findPending()).thenReturn(List.of(request));

        mockMvc.perform(get("/api/staff/owner-registration-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(8))
                .andExpect(jsonPath("$[0].email").value("luis@example.com"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void approveReturnsCreatedOwner() throws Exception {
        Owner owner = mock(Owner.class);
        when(owner.getId()).thenReturn(11L);
        when(owner.getFullName()).thenReturn("Luis Gomez");
        when(owner.getPhone()).thenReturn("555-0200");
        when(owner.getEmail()).thenReturn("luis@example.com");
        when(owner.getAddress()).thenReturn("Avenida 2");
        when(registrations.approve(8L)).thenReturn(owner);

        mockMvc.perform(post("/api/staff/owner-registration-requests/8/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    void rejectDelegatesToServiceAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/staff/owner-registration-requests/8"))
                .andExpect(status().isNoContent());

        verify(registrations).reject(8L);
    }
}
