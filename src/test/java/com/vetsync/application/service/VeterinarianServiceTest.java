package com.vetsync.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vetsync.application.exception.ResourceNotFoundException;
import com.vetsync.domain.model.Veterinarian;
import com.vetsync.domain.repository.VeterinarianRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VeterinarianServiceTest {

    @Test
    void createPersistsAndReturnsSavedVeterinarian() {
        VeterinarianRepository repository = mock(VeterinarianRepository.class);
        Veterinarian saved = new Veterinarian("Ana Ruiz", "ana@vet.com", "Cirugia");
        when(repository.save(any(Veterinarian.class))).thenReturn(saved);
        VeterinarianService service = new VeterinarianService(repository);

        Veterinarian result = service.create("Ana Ruiz", "ana@vet.com", "Cirugia");

        assertThat(result).isSameAs(saved);
    }

    @Test
    void findAllReturnsRepositoryContents() {
        VeterinarianRepository repository = mock(VeterinarianRepository.class);
        List<Veterinarian> expected = List.of(new Veterinarian("Ana Ruiz", "ana@vet.com", "Cirugia"));
        when(repository.findAll()).thenReturn(expected);
        VeterinarianService service = new VeterinarianService(repository);

        assertThat(service.findAll()).isSameAs(expected);
    }

    @Test
    void getReturnsVeterinarianWhenPresent() {
        VeterinarianRepository repository = mock(VeterinarianRepository.class);
        Veterinarian vet = new Veterinarian("Ana Ruiz", "ana@vet.com", "Cirugia");
        when(repository.findById(3L)).thenReturn(Optional.of(vet));
        VeterinarianService service = new VeterinarianService(repository);

        assertThat(service.get(3L)).isSameAs(vet);
    }

    @Test
    void getThrowsNotFoundWhenMissing() {
        VeterinarianRepository repository = mock(VeterinarianRepository.class);
        when(repository.findById(99L)).thenReturn(Optional.empty());
        VeterinarianService service = new VeterinarianService(repository);

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
