package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DeepblueRescueApplicationTests {

    @Test
    void contextLoads() {
    }

    @ExtendWith(MockitoExtension.class)
    static
    class TreatmentServiceImplTest {

        @Mock
        private AnimalRepository animalRepository;

        @Mock
        private SpecialistRepository specialistRepository;

        @Mock
        private TreatmentRepository treatmentRepository;

        @Mock
        private TreatmentMapper mapper;

        @InjectMocks
        private TreatmentServiceImpl service;

        // ---------------------------------------------------------
        // Paso 32 — Test de registro correcto
        // ---------------------------------------------------------
        @Test
        void shouldRegisterTreatmentWhenRequestIsValid() {

            // ARRANGE
            RescueCase rescueCase = new RescueCase(
                    "RES-001",
                    LocalDate.of(2026, 8, 20),
                    "Santa Marta Bay",
                    RescueStatus.IN_REHABILITATION
            );

            Animal animal = new Animal(
                    "AN-001",
                    "Green Sea Turtle",
                    "Chelonia mydas",
                    AnimalSex.FEMALE
            );
            rescueCase.assignAnimal(animal);

            Specialist specialist = new Specialist(
                    "SPEC-001",
                    "Elena",
                    "Vargas",
                    "[email protected]"
            );
            // specialist.active = true por defecto

            CreateTreatmentRequest request = new CreateTreatmentRequest(
                    "AN-001",
                    "SPEC-001",
                    LocalDateTime.of(2026, 8, 21, 9, 0),
                    TreatmentType.WOUND_CARE,
                    "Cleaning of left front flipper injury."
            );

            Treatment treatment = new Treatment(
                    animal,
                    specialist,
                    request.performedAt(),
                    request.type(),
                    request.description()
            );

            TreatmentResponse response = new TreatmentResponse(
                    1L,
                    "AN-001",
                    "SPEC-001",
                    request.performedAt(),
                    TreatmentType.WOUND_CARE,
                    request.description()
            );

            when(animalRepository.findByAnimalCode("AN-001"))
                    .thenReturn(Optional.of(animal));

            when(specialistRepository.findByProfessionalCode("SPEC-001"))
                    .thenReturn(Optional.of(specialist));

            when(treatmentRepository.save(any(Treatment.class)))
                    .thenReturn(treatment);

            when(mapper.toResponse(treatment))
                    .thenReturn(response);

            // ACT
            TreatmentResponse result = service.register(request);

            // ASSERT
            assertThat(result).isEqualTo(response);

            verify(treatmentRepository).save(any(Treatment.class));
        }

        // ---------------------------------------------------------
        // Paso 33 — Test especialista inactivo
        // ---------------------------------------------------------
        @Test
        void shouldThrowBusinessRuleExceptionWhenSpecialistIsInactive() {

            // ARRANGE
            RescueCase rescueCase = new RescueCase(
                    "RES-001",
                    LocalDate.of(2026, 8, 20),
                    "Santa Marta Bay",
                    RescueStatus.IN_REHABILITATION
            );

            Animal animal = new Animal(
                    "AN-001",
                    "Green Sea Turtle",
                    "Chelonia mydas",
                    AnimalSex.FEMALE
            );
            rescueCase.assignAnimal(animal);

            Specialist specialist = new Specialist(
                    "SPEC-001",
                    "Elena",
                    "Vargas",
                    "[email protected]"
            );
            // Specialist no expone un setter público para "active",
            // así que forzamos el campo privado con ReflectionTestUtils.
            ReflectionTestUtils.setField(specialist, "active", false);

            CreateTreatmentRequest request = new CreateTreatmentRequest(
                    "AN-001",
                    "SPEC-001",
                    LocalDateTime.of(2026, 8, 21, 9, 0),
                    TreatmentType.WOUND_CARE,
                    "Cleaning of left front flipper injury."
            );

            when(animalRepository.findByAnimalCode("AN-001"))
                    .thenReturn(Optional.of(animal));

            when(specialistRepository.findByProfessionalCode("SPEC-001"))
                    .thenReturn(Optional.of(specialist));

            // ACT + ASSERT
            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOf(BusinessRuleException.class);

            verify(treatmentRepository, never()).save(any());
        }

        // ---------------------------------------------------------
        // Paso 34 — Test animal liberado
        // ---------------------------------------------------------
        @Test
        void shouldThrowBusinessRuleExceptionWhenCaseIsReleased() {

            // ARRANGE
            RescueCase rescueCase = new RescueCase(
                    "RES-001",
                    LocalDate.of(2026, 8, 20),
                    "Santa Marta Bay",
                    RescueStatus.RELEASED
            );

            Animal animal = new Animal(
                    "AN-001",
                    "Green Sea Turtle",
                    "Chelonia mydas",
                    AnimalSex.FEMALE
            );
            rescueCase.assignAnimal(animal);

            Specialist specialist = new Specialist(
                    "SPEC-001",
                    "Elena",
                    "Vargas",
                    "[email protected]"
            );
            // specialist.active = true por defecto

            CreateTreatmentRequest request = new CreateTreatmentRequest(
                    "AN-001",
                    "SPEC-001",
                    LocalDateTime.of(2026, 8, 21, 9, 0),
                    TreatmentType.OBSERVATION,
                    "Routine check after release."
            );

            when(animalRepository.findByAnimalCode("AN-001"))
                    .thenReturn(Optional.of(animal));

            when(specialistRepository.findByProfessionalCode("SPEC-001"))
                    .thenReturn(Optional.of(specialist));

            // ACT + ASSERT
            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOf(BusinessRuleException.class);

            verify(treatmentRepository, never()).save(any());
        }
    }
}
