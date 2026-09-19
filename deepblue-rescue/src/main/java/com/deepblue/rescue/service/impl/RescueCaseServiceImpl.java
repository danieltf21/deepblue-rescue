package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.RescueCaseService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RescueCaseServiceImpl implements RescueCaseService {

    private final RescueCaseRepository repository;
    private final RescueCaseMapper mapper;

    public RescueCaseServiceImpl(
            RescueCaseRepository repository,
            RescueCaseMapper mapper) {

        this.repository = repository;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------
    // Paso 15 — findByCode
    // ---------------------------------------------------------
    @Override
    public RescueCaseResponse findByCode(String caseCode) {

        return repository
                .findByCaseCode(caseCode)
                .map(mapper::toResponse)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Rescue case not found: " + caseCode
                        )
                );
    }

    // ---------------------------------------------------------
    // Paso 16 — findByStatus
    // ---------------------------------------------------------
    @Override
    public List<RescueCaseResponse> findByStatus(RescueStatus status) {

        return repository
                .findByStatusOrderByRescueDateAsc(status)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    // ---------------------------------------------------------
    // Paso 19 — changeStatus
    // ---------------------------------------------------------
    @Override
    @Transactional
    public RescueCaseResponse changeStatus(
            String caseCode,
            ChangeRescueStatusRequest request) {

        // TODO 1 — Buscar el RescueCase.
        RescueCase rescueCase = repository
                .findByCaseCode(caseCode)
                // TODO 2 — Si no existe: ResourceNotFoundException.
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Rescue case not found: " + caseCode
                        )
                );

        // TODO 3 — Obtener currentStatus.
        RescueStatus currentStatus = rescueCase.getStatus();
        RescueStatus newStatus = request.status();

        // TODO 4 — Validar transición.
        boolean valid = isValidTransition(currentStatus, newStatus);

        // TODO 5 — Si no es válida: BusinessRuleException.
        if (!valid) {
            throw new BusinessRuleException(
                    "Invalid status transition: "
                            + currentStatus + " -> " + newStatus
            );
        }

        // TODO 6 — Cambiar status.
        rescueCase.setStatus(newStatus);

        // TODO 7 — Guardar.
        RescueCase saved = repository.save(rescueCase);

        // TODO 8 — Transformar a Response.
        return mapper.toResponse(saved);
    }

    // ---------------------------------------------------------
    // Paso 18 — Validación de transición de estados
    // ---------------------------------------------------------
    private boolean isValidTransition(RescueStatus current, RescueStatus next) {

        return switch (current) {

            case ADMITTED ->
                    next == RescueStatus.UNDER_EVALUATION;

            case UNDER_EVALUATION ->
                    next == RescueStatus.IN_REHABILITATION;

            case IN_REHABILITATION ->
                    next == RescueStatus.READY_FOR_RELEASE;

            case READY_FOR_RELEASE ->
                    next == RescueStatus.RELEASED;

            default -> false;
        };
    }
}