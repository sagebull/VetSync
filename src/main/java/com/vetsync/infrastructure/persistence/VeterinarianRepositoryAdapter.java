package com.vetsync.infrastructure.persistence;

import com.vetsync.domain.model.Veterinarian;
import com.vetsync.domain.repository.VeterinarianRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class VeterinarianRepositoryAdapter implements VeterinarianRepository {
    private final SpringDataVeterinarianRepository veterinarians;

    public VeterinarianRepositoryAdapter(SpringDataVeterinarianRepository veterinarians) {
        this.veterinarians = veterinarians;
    }

    @Override
    public Optional<Veterinarian> findById(Long id) {
        return veterinarians.findById(id);
    }

    @Override
    public Optional<Veterinarian> findByIdForUpdate(Long id) {
        return veterinarians.findByIdForUpdate(id);
    }

    @Override
    public List<Veterinarian> findAll() {
        return veterinarians.findAll();
    }

    @Override
    public Veterinarian save(Veterinarian veterinarian) {
        return veterinarians.save(veterinarian);
    }
}
