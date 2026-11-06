package com.vetsync.api.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.api.exception.ApiExceptionHandler;
import com.vetsync.application.service.PetService;
import com.vetsync.api.security.AccessAuthorization;
import com.vetsync.domain.model.Owner;
import com.vetsync.domain.model.Pet;
import com.vetsync.domain.model.Sex;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PetControllerTest {

    private final PetService pets = mock(PetService.class);
    private final AccessAuthorization accessAuthorization = mock(AccessAuthorization.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PetController(pets, accessAuthorization))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    private static Pet pet(long id) {
        Owner owner = mock(Owner.class);
        when(owner.getId()).thenReturn(5L);
        Pet pet = mock(Pet.class);
        when(pet.getId()).thenReturn(id);
        when(pet.getName()).thenReturn("Luna");
        when(pet.getSpecies()).thenReturn("Gato");
        when(pet.getBreed()).thenReturn("Siames");
        when(pet.getSex()).thenReturn(Sex.FEMALE);
        when(pet.getBirthDate()).thenReturn(LocalDate.of(2020, 5, 1));
        when(pet.getWeight()).thenReturn(new BigDecimal("4.20"));
        when(pet.getObservations()).thenReturn("Ninguna");
        when(pet.getOwner()).thenReturn(owner);
        return pet;
    }

    @Test
    void createReturnsPetResponse() throws Exception {
        Pet expected = pet(10L);
        when(pets.create("Luna", "Gato", "Siames", Sex.FEMALE, LocalDate.of(2020, 5, 1),
                new BigDecimal("4.20"), "Ninguna", 5L)).thenReturn(expected);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/pets").contentType("application/json").content("""
                                {"name":"Luna","species":"Gato","breed":"Siames","sex":"FEMALE",
                                 "birthDate":"2020-05-01","weight":4.20,"observations":"Ninguna","ownerId":5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.ownerId").value(5))
                .andExpect(jsonPath("$.sex").value("FEMALE"));
    }

    @Test
    void getReturnsPetById() throws Exception {
        Pet expected = pet(10L);
        when(pets.get(10L)).thenReturn(expected);

        mockMvc.perform(get("/api/pets/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luna"));
    }

    @Test
    void searchPassesFiltersToService() throws Exception {
        List<Pet> expected = List.of(pet(10L));
        when(accessAuthorization.resolveOwnerScope(null, null)).thenReturn(null);
        when(pets.search("Luna", null, null, null)).thenReturn(expected);

        mockMvc.perform(get("/api/pets").param("name", "Luna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].species").value("Gato"));
    }
}
