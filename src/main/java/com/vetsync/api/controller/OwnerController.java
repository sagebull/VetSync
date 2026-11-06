package com.vetsync.api.controller;

import com.vetsync.api.dto.OwnerRequest;
import com.vetsync.api.dto.OwnerResponse;
import com.vetsync.application.service.OwnerService;
import com.vetsync.domain.model.Owner;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/owners")
public class OwnerController {
    private final OwnerService owners;

    public OwnerController(OwnerService owners) {
        this.owners = owners;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STAFF')")
    public OwnerResponse create(@Valid @RequestBody OwnerRequest request) {
        return toResponse(owners.create(request.fullName(), request.phone(), request.email(), request.address()));
    }

    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    public List<OwnerResponse> findAll() {
        return owners.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessAuthorization.canAccessOwner(authentication, #id)")
    public OwnerResponse get(@PathVariable Long id) {
        return toResponse(owners.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessAuthorization.canAccessOwner(authentication, #id)")
    public OwnerResponse update(@PathVariable Long id, @Valid @RequestBody OwnerRequest request) {
        return toResponse(owners.update(id, request.fullName(), request.phone(), request.email(), request.address()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('STAFF')")
    public void delete(@PathVariable Long id) {
        owners.delete(id);
    }

    private OwnerResponse toResponse(Owner owner) {
        return new OwnerResponse(owner.getId(), owner.getFullName(), owner.getPhone(), owner.getEmail(), owner.getAddress());
    }
}
