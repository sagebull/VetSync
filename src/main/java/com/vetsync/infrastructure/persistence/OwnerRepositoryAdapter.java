package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.Owner;
import com.vetsync.domain.repository.OwnerRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OwnerRepositoryAdapter implements OwnerRepository {
    private final SpringDataOwnerRepository owners;
    private final SpringDataPetRepository pets;

    public OwnerRepositoryAdapter(SpringDataOwnerRepository owners, SpringDataPetRepository pets) {
        this.owners = owners;
        this.pets = pets;
    }

    @Override
    public List<Owner> findAll() {
        return owners.findAll();
    }

    @Override
    public Optional<Owner> findById(Long id) {
        return owners.findById(id);
    }

    @Override
    public Owner save(Owner owner) {
        return owners.save(owner);
    }

    @Override
    public void delete(Owner owner) {
        owners.delete(owner);
    }

    @Override
    public boolean hasPets(Long ownerId) {
        return pets.existsByOwner_Id(ownerId);
    }
}
