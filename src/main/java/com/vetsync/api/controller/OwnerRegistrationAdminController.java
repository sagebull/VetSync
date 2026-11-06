package com.vetsync.api.controller;

import com.vetsync.api.dto.OwnerRegistrationPendingResponse;
import com.vetsync.api.dto.OwnerResponse;
import com.vetsync.application.service.OwnerRegistrationService;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.OwnerRegistration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/owner-registration-requests")
@PreAuthorize("hasRole('STAFF')")
public class OwnerRegistrationAdminController {
    private final OwnerRegistrationService registrations;

    public OwnerRegistrationAdminController(OwnerRegistrationService registrations) {
        this.registrations = registrations;
    }

    @GetMapping
    public List<OwnerRegistrationPendingResponse> findPending() {
        return registrations.findPending().stream().map(this::toResponse).toList();
    }

    @PostMapping("/{requestId}/approve")
    public OwnerResponse approve(@PathVariable Long requestId) {
        Owner owner = registrations.approve(requestId);
        return new OwnerResponse(owner.getId(), owner.getFullName(), owner.getPhone(), owner.getEmail(),
                owner.getAddress());
    }

    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long requestId) {
        registrations.reject(requestId);
    }

    private OwnerRegistrationPendingResponse toResponse(OwnerRegistration request) {
        return new OwnerRegistrationPendingResponse(request.getId(), request.getFullName(), request.getPhone(),
                request.getEmail(), request.getAddress(), request.getCreatedAt());
    }
}
