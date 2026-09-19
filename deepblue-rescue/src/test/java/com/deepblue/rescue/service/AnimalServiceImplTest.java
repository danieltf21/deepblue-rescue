package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import com.deepblue.rescue.mapper.AnimalMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    void shouldReturnTrueWhenCaseIsInRehabilitation() {

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

        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        // ACT
        boolean result = service.canReceiveTreatment("AN-001");

        // ASSERT
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenCaseIsReleased() {

        // ARRANGE
        RescueCase rescueCase = new RescueCase(
                "RES-002",
                LocalDate.of(2026, 8, 20),
                "Santa Marta Bay",
                RescueStatus.RELEASED
        );

        Animal animal = new Animal(
                "AN-002",
                "Loggerhead Turtle",
                "Caretta caretta",
                AnimalSex.MALE
        );
        rescueCase.assignAnimal(animal);

        when(repository.findByAnimalCode("AN-002"))
                .thenReturn(Optional.of(animal));

        // ACT
        boolean result = service.canReceiveTreatment("AN-002");

        // ASSERT
        assertThat(result).isFalse();
    }
}