package com.vetsync.application.service;

import com.vetsync.application.exception.ResourceConflictException;
import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.repository.OwnerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerService {
    private final OwnerRepository owners;

    public OwnerService(OwnerRepository owners) {
        this.owners = owners;
    }

    @Transactional
    public Owner create(String fullName, String phone, String email, String address) {
        return owners.save(new Owner(fullName, phone, email, address));
    }

    @Transactional(readOnly = true)
    public List<Owner> findAll() {
        return owners.findAll();
    }

    @Transactional(readOnly = true)
    public Owner get(Long id) {
        return findOwner(id);
    }

    @Transactional
    public Owner update(Long id, String fullName, String phone, String email, String address) {
        Owner owner = findOwner(id);
        owner.update(fullName, phone, email, address);
        return owners.save(owner);
    }

    @Transactional
    public void delete(Long id) {
        Owner owner = findOwner(id);
        if (owners.hasPets(id)) {
            throw new ResourceConflictException("No se puede eliminar un propietario que tiene mascotas registradas");
        }
        owners.delete(owner);
    }

    private Owner findOwner(Long id) {
        return owners.findById(id).orElseThrow(() -> new ResourceNotFoundException("Propietario", id));
    }
}
