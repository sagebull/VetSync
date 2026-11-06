package com.vetsync.api.controller;

import com.vetsync.api.dto.VeterinarianRequest;
import com.vetsync.api.dto.VeterinarianResponse;
import com.vetsync.application.service.VeterinarianService;
import com.vetsync.domain.model.Veterinarian;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/veterinarians")
public class VeterinarianController {
    private final VeterinarianService veterinarians;

    public VeterinarianController(VeterinarianService veterinarians) {
        this.veterinarians = veterinarians;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STAFF')")
    public VeterinarianResponse create(@Valid @RequestBody VeterinarianRequest request) {
        return toResponse(veterinarians.create(request.fullName(), request.email(), request.specialty()));
    }

    @GetMapping
    public List<VeterinarianResponse> findAll() {
        return veterinarians.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public VeterinarianResponse get(@PathVariable Long id) {
        return toResponse(veterinarians.get(id));
    }

    private VeterinarianResponse toResponse(Veterinarian veterinarian) {
        return new VeterinarianResponse(veterinarian.getId(), veterinarian.getFullName(),
                veterinarian.getEmail(), veterinarian.getSpecialty());
    }
}
