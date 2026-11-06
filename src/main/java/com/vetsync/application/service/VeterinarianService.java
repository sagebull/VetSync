package com.vetsync.application.service;

import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.Veterinarian;
import com.vetsync.domain.repository.VeterinarianRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VeterinarianService {
    private final VeterinarianRepository veterinarians;

    public VeterinarianService(VeterinarianRepository veterinarians) {
        this.veterinarians = veterinarians;
    }

    @Transactional
    public Veterinarian create(String fullName, String email, String specialty) {
        return veterinarians.save(new Veterinarian(fullName, email, specialty));
    }

    @Transactional(readOnly = true)
    public List<Veterinarian> findAll() {
        return veterinarians.findAll();
    }

    @Transactional(readOnly = true)
    public Veterinarian get(Long id) {
        return veterinarians.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario", id));
    }
}
