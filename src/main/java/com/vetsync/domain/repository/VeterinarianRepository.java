package com.vetsync.domain.repository;

import com.vetsync.domain.model.Veterinarian;
import java.util.List;
import java.util.Optional;

public interface VeterinarianRepository {
    Optional<Veterinarian> findById(Long id);

    Optional<Veterinarian> findByIdForUpdate(Long id);

    List<Veterinarian> findAll();

    Veterinarian save(Veterinarian veterinarian);
}
