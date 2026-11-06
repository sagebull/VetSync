package com.vetsync.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsync.application.exception.ResourceConflictException;
import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.repository.OwnerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {
    @Mock
    private OwnerRepository owners;

    @InjectMocks
    private OwnerService service;

    @Test
    void createPersistsOwnerDetails() {
        Owner saved = new Owner("Ana Lopez", "+52 555 123 4567", "ana@example.com", "Calle 1");
        when(owners.save(org.mockito.ArgumentMatchers.any(Owner.class))).thenReturn(saved);

        Owner result = service.create("Ana Lopez", "+52 555 123 4567", "ana@example.com", "Calle 1");

        assertEquals("ana@example.com", result.getEmail());
        verify(owners).save(org.mockito.ArgumentMatchers.any(Owner.class));
    }

    @Test
    void deleteRejectsOwnerWithPets() {
        Owner owner = new Owner("Ana Lopez", "5551234567", "ana@example.com", "Calle 1");
        when(owners.findById(7L)).thenReturn(Optional.of(owner));
        when(owners.hasPets(7L)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> service.delete(7L));
    }

    @Test
    void getRejectsUnknownOwner() {
        when(owners.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(404L));
    }
}
