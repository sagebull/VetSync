package com.vetsync.api.controller;

import com.vetsync.api.dto.PetRequest;
import com.vetsync.api.dto.PetResponse;
import com.vetsync.application.service.PetService;
import com.vetsync.domain.model.Pet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.vetsync.api.security.AccessAuthorization;

@RestController
@RequestMapping("/api/pets")
public class PetController {
    private final PetService pets;
    private final AccessAuthorization accessAuthorization;

    public PetController(PetService pets, AccessAuthorization accessAuthorization) {
        this.pets = pets;
        this.accessAuthorization = accessAuthorization;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@accessAuthorization.canAccessOwner(authentication, #request.ownerId())")
    public PetResponse create(@Valid @RequestBody PetRequest request) {
        return toResponse(pets.create(request.name(), request.species(), request.breed(), request.sex(),
                request.birthDate(), request.weight(), request.observations(), request.ownerId()));
    }

    @GetMapping
    public List<PetResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) @Positive Long ownerId,
            @RequestParam(required = false) @Positive Long id) {
        Long ownerScope = accessAuthorization.resolveOwnerScope(jwt, ownerId);
        return pets.search(name, ownerName, ownerScope, id).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessAuthorization.canAccessPet(authentication, #id)")
    public PetResponse get(@PathVariable Long id) {
        return toResponse(pets.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessAuthorization.canManagePetAndOwner(authentication, #id, #request.ownerId())")
    public PetResponse update(@PathVariable Long id, @Valid @RequestBody PetRequest request) {
        return toResponse(pets.update(id, request.name(), request.species(), request.breed(), request.sex(),
                request.birthDate(), request.weight(), request.observations(), request.ownerId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@accessAuthorization.canAccessPet(authentication, #id)")
    public void delete(@PathVariable Long id) {
        pets.delete(id);
    }

    private PetResponse toResponse(Pet pet) {
        return new PetResponse(pet.getId(), pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getSex(),
                pet.getBirthDate(), pet.getWeight(), pet.getObservations(), pet.getOwner().getId());
    }
}
