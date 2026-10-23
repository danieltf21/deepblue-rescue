package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.TreatmentType; // ajusta al paquete de tu enum
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalController.class)
@Import(GlobalExceptionHandler.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @MockitoBean
    private TreatmentService treatmentService;

    private AnimalResponse buildAnimal(String code) {
        return new AnimalResponse(
                1L,
                code,
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.values()[0],
                "RES-2026-001",
                RescueStatus.IN_REHABILITATION
        );
    }

    // Punto 79: GET /api/animals/{animalCode}
    @Test
    void shouldReturnAnimalByCode() throws Exception {

        when(animalService.findByCode("AN-001"))
                .thenReturn(buildAnimal("AN-001"));

        mockMvc.perform(get("/api/animals/{code}", "AN-001"))
                .andExpect(status().isOk());

        verify(animalService).findByCode("AN-001");
    }

    // Punto 80: GET /api/animals/in-rehabilitation
    @Test
    void shouldReturnAnimalsInRehabilitation() throws Exception {

        when(animalService.findAnimalsInRehabilitation())
                .thenReturn(List.of(buildAnimal("AN-001"), buildAnimal("AN-002")));

        mockMvc.perform(get("/api/animals/in-rehabilitation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(animalService).findAnimalsInRehabilitation();
    }

    // Punto 81: GET /api/animals/{animalCode}/treatments
    @Test
    void shouldReturnAnimalTreatments() throws Exception {

        TreatmentResponse treatment = new TreatmentResponse(
                100L,
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning and treatment of flipper injury."
        );

        when(treatmentService.findByAnimalCode("AN-001"))
                .thenReturn(List.of(treatment));

        mockMvc.perform(get("/api/animals/{code}/treatments", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].animalCode").value("AN-001"));

        verify(treatmentService).findByAnimalCode("AN-001");
    }

    // Punto 82: GET /api/animals/{animalCode}/treatment-eligibility
    @Test
    void shouldReturnTreatmentEligibility() throws Exception {

        when(animalService.canReceiveTreatment("AN-001")).thenReturn(true);

        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.eligible").value(true));

        verify(animalService).canReceiveTreatment("AN-001");
    }

    // Caso 16 de los tests mínimos: animal inexistente
    @Test
    void shouldReturn404WhenAnimalDoesNotExist() throws Exception {

        when(animalService.findByCode("AN-999"))
                .thenThrow(new ResourceNotFoundException("Animal not found: AN-999"));

        mockMvc.perform(get("/api/animals/{code}", "AN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"))
                .andExpect(jsonPath("$.details").isMap());
    }
    // Reto, punto 91: eligibility con animal inexistente
    @Test
    void shouldReturn404WhenEligibilityAnimalDoesNotExist() throws Exception {

        when(animalService.canReceiveTreatment("AN-999"))
                .thenThrow(new ResourceNotFoundException("Animal not found: AN-999"));

        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"))
                .andExpect(jsonPath("$.details").isMap());

        verify(animalService).canReceiveTreatment("AN-999");
    }
}