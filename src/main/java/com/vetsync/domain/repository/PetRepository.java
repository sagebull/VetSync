package com.vetsync.domain.repository;

import com.vetsync.domain.model.Pet;
import java.util.List;
import java.util.Optional;

public interface PetRepository {
    Optional<Pet> findById(Long id);

    boolean existsByIdAndOwnerId(Long petId, Long ownerId);

    List<Pet> search(String name, String ownerName, Long ownerId, Long petId);

    Pet save(Pet pet);

    void delete(Pet pet);

    boolean hasMedicalRecords(Long petId);
}
