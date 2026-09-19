package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TreatmentServiceImpl implements TreatmentService {

    private final AnimalRepository animalRepository;
    private final SpecialistRepository specialistRepository;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper mapper;

    public TreatmentServiceImpl(
            AnimalRepository animalRepository,
            SpecialistRepository specialistRepository,
            TreatmentRepository treatmentRepository,
            TreatmentMapper mapper) {

        this.animalRepository = animalRepository;
        this.specialistRepository = specialistRepository;
        this.treatmentRepository = treatmentRepository;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------
    // Paso 24 — findByAnimalCode
    // ---------------------------------------------------------
    @Override
    public List<TreatmentResponse> findByAnimalCode(String animalCode) {

        return treatmentRepository
                .findByAnimalAnimalCodeOrderByPerformedAtAsc(animalCode)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    // ---------------------------------------------------------
    // Paso 25 — register (con las 5 reglas de negocio)
    // ---------------------------------------------------------
    @Override
    @Transactional
    public TreatmentResponse register(CreateTreatmentRequest request) {

        // 1. Buscar Animal
        Animal animal = animalRepository
                .findByAnimalCode(request.animalCode())
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Animal not found: " + request.animalCode()
                        )
                );

        // 2. Buscar Specialist
        Specialist specialist = specialistRepository
                .findByProfessionalCode(request.specialistCode())
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Specialist not found: " + request.specialistCode()
                        )
                );

        // 3. Validar specialist.active
        if (!specialist.isActive()) {
            throw new BusinessRuleException(
                    "Specialist is not active: " + request.specialistCode()
            );
        }

        // 4. Obtener RescueCase del Animal
        // ASUNCION: Animal tiene el metodo getRescueCase().
        // Si tu entidad usa otro nombre, ajustalo aqui.
        RescueCase rescueCase = animal.getRescueCase();

        // 5. Validar status
        // ASUNCION: RescueStatus tiene el valor CLOSED.
        // Si no existe en tu enum, quita esa condicion o
        // reemplazala por los valores reales que uses.
        if (rescueCase.getStatus() == RescueStatus.RELEASED
                || rescueCase.getStatus() == RescueStatus.CLOSED) {

            throw new BusinessRuleException(
                    "Cannot register treatment because the case is "
                            + rescueCase.getStatus()
            );
        }

        // 6. Validar performedAt
        if (request.performedAt().toLocalDate().isBefore(rescueCase.getRescueDate())) {
            throw new BusinessRuleException(
                    "Treatment date cannot be before the rescue date"
            );
        }

        // 7. Crear Treatment
        Treatment treatment = new Treatment(
                animal,
                specialist,
                request.performedAt(),
                request.type(),
                request.description()
        );

        // 8. Guardar Treatment
        Treatment saved = treatmentRepository.save(treatment);

        // 9. Mapear TreatmentResponse
        return mapper.toResponse(saved);
    }
}