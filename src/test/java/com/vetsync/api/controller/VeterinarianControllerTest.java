package com.vetsync.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.api.exception.ApiExceptionHandler;
import com.vetsync.application.service.VeterinarianService;
import com.vetsync.domain.model.Veterinarian;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VeterinarianControllerTest {

    private final VeterinarianService veterinarians = mock(VeterinarianService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new VeterinarianController(veterinarians))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    private static Veterinarian veterinarian(long id) {
        Veterinarian vet = mock(Veterinarian.class);
        when(vet.getId()).thenReturn(id);
        when(vet.getFullName()).thenReturn("Dra. Lopez");
        when(vet.getEmail()).thenReturn("lopez@example.com");
        when(vet.getSpecialty()).thenReturn("Felinos");
        return vet;
    }

    @Test
    void createReturnsVeterinarianResponse() throws Exception {
        Veterinarian expected = veterinarian(3L);
        when(veterinarians.create("Dra. Lopez", "lopez@example.com", "Felinos")).thenReturn(expected);

        mockMvc.perform(post("/api/veterinarians").contentType("application/json").content("""
                        {"fullName":"Dra. Lopez","email":"lopez@example.com","specialty":"Felinos"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.specialty").value("Felinos"));
    }

    @Test
    void findAllReturnsAllVeterinarians() throws Exception {
        List<Veterinarian> expected = List.of(veterinarian(1L));
        when(veterinarians.findAll()).thenReturn(expected);

        mockMvc.perform(get("/api/veterinarians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Dra. Lopez"));
    }

    @Test
    void getReturnsVeterinarianById() throws Exception {
        Veterinarian expected = veterinarian(3L);
        when(veterinarians.get(3L)).thenReturn(expected);

        mockMvc.perform(get("/api/veterinarians/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lopez@example.com"));
    }
}
