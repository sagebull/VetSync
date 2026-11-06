package com.vetsync.application.service;

import com.vetsync.application.exception.ResourceConflictException;
import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.model.Sex;
import com.vetsync.domain.repository.OwnerRepository;
import com.vetsync.domain.repository.PetRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetService {
    private final PetRepository pets;
    private final OwnerRepository owners;

    public PetService(PetRepository pets, OwnerRepository owners) {
        this.pets = pets;
        this.owners = owners;
    }

    @Transactional
    public Pet create(String name, String species, String breed, Sex sex, LocalDate birthDate,
                      BigDecimal weight, String observations, Long ownerId) {
        Owner owner = findOwner(ownerId);
        return pets.save(new Pet(name, species, breed, sex, birthDate, weight, observations, owner));
    }

    @Transactional(readOnly = true)
    public Pet get(Long id) {
        return findPet(id);
    }

    @Transactional(readOnly = true)
    public List<Pet> search(String name, String ownerName, Long ownerId, Long petId) {
        return pets.search(normalize(name), normalize(ownerName), ownerId, petId);
    }

    @Transactional
    public Pet update(Long id, String name, String species, String breed, Sex sex, LocalDate birthDate,
                      BigDecimal weight, String observations, Long ownerId) {
        Pet pet = findPet(id);
        Owner owner = findOwner(ownerId);
        pet.update(name, species, breed, sex, birthDate, weight, observations, owner);
        return pets.save(pet);
    }

    @Transactional
    public void delete(Long id) {
        Pet pet = findPet(id);
        if (pets.hasMedicalRecords(id)) {
            throw new ResourceConflictException("No se puede eliminar una mascota con historial medico");
        }
        pets.delete(pet);
    }

    private Pet findPet(Long id) {
        return pets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mascota", id));
    }

    private Owner findOwner(Long id) {
        return owners.findById(id).orElseThrow(() -> new ResourceNotFoundException("Propietario", id));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
