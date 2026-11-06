package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.Pet;
import com.vetsync.domain.repository.PetRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PetRepositoryAdapter implements PetRepository {
    private final SpringDataPetRepository pets;
    private final SpringDataMedicalRecordRepository medicalRecords;

    public PetRepositoryAdapter(SpringDataPetRepository pets, SpringDataMedicalRecordRepository medicalRecords) {
        this.pets = pets;
        this.medicalRecords = medicalRecords;
    }

    @Override
    public Optional<Pet> findById(Long id) {
        return pets.findById(id);
    }

    @Override
    public boolean existsByIdAndOwnerId(Long petId, Long ownerId) {
        return pets.existsByIdAndOwner_Id(petId, ownerId);
    }

    @Override
    public List<Pet> search(String name, String ownerName, Long ownerId, Long petId) {
        return pets.search(name, ownerName, ownerId, petId);
    }

    @Override
    public Pet save(Pet pet) {
        return pets.save(pet);
    }

    @Override
    public void delete(Pet pet) {
        pets.delete(pet);
    }

    @Override
    public boolean hasMedicalRecords(Long petId) {
        return medicalRecords.existsByPet_Id(petId);
    }
}
