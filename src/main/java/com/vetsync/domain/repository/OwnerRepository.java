package com.vetsync.domain.repository;

import com.vetsync.domain.model.Owner;
import java.util.List;
import java.util.Optional;

public interface OwnerRepository {
    List<Owner> findAll();

    Optional<Owner> findById(Long id);

    Owner save(Owner owner);

    void delete(Owner owner);

    boolean hasPets(Long ownerId);
}
