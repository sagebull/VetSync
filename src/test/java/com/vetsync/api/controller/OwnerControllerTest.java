package com.vetsync.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.api.exception.ApiExceptionHandler;
import com.vetsync.api.security.AccessAuthorization;
import com.vetsync.application.exception.ResourceConflictException;
import com.vetsync.application.service.OwnerService;
import com.vetsync.application.service.PetService;
import com.vetsync.application.service.VeterinarianService;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.model.Sex;
import com.vetsync.domain.model.Veterinarian;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OwnerControllerTest {

    private final OwnerService owners = mock(OwnerService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OwnerController(owners))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    private static Owner owner(long id) {
        Owner owner = mock(Owner.class);
        when(owner.getId()).thenReturn(id);
        when(owner.getFullName()).thenReturn("Ana Perez");
        when(owner.getPhone()).thenReturn("555-0100");
        when(owner.getEmail()).thenReturn("ana@example.com");
        when(owner.getAddress()).thenReturn("Calle 1");
        return owner;
    }

    private static final String VALID_OWNER_JSON = """
            {"fullName":"Ana Perez","phone":"555-0100","email":"ana@example.com","address":"Calle 1"}
            """;

    @Test
    void createReturnsOwnerResponse() throws Exception {
        Owner expected = owner(4L);
        when(owners.create("Ana Perez", "555-0100", "ana@example.com", "Calle 1")).thenReturn(expected);

        mockMvc.perform(post("/api/owners").contentType("application/json").content(VALID_OWNER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void createRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/owners").contentType("application/json").content("""
                        {"fullName":"Ana","phone":"1","email":"no-es-correo","address":"x"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void findAllReturnsAllOwners() throws Exception {
        List<Owner> expected = List.of(owner(1L), owner(2L));
        when(owners.findAll()).thenReturn(expected);

        mockMvc.perform(get("/api/owners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getReturnsOwnerById() throws Exception {
        Owner expected = owner(4L);
        when(owners.get(4L)).thenReturn(expected);

        mockMvc.perform(get("/api/owners/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Perez"));
    }

    @Test
    void updateReturnsUpdatedOwner() throws Exception {
        Owner expected = owner(4L);
        when(owners.update(4L, "Ana Perez", "555-0100", "ana@example.com", "Calle 1")).thenReturn(expected);

        mockMvc.perform(put("/api/owners/4").contentType("application/json").content(VALID_OWNER_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void deleteInvokesServiceAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/owners/4"))
                .andExpect(status().isNoContent());

        verify(owners).delete(4L);
    }

    @Test
    void deleteReturnsConflictWhenOwnerHasPets() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceConflictException("Propietario con mascotas"))
                .when(owners).delete(4L);

        mockMvc.perform(delete("/api/owners/4"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Propietario con mascotas"));
    }
}
