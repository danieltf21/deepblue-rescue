package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.service.RescueCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(RescueCaseController.class)
@Import(GlobalExceptionHandler.class)
class RescueCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RescueCaseService service;
    @Test
    void shouldReturnRescueCaseByCode()
            throws Exception {
        RescueCaseResponse response =
                new RescueCaseResponse(
                        1L,
                        "RES-2026-001",
                        LocalDate.of(
                                2026,
                                8,
                                20
                        ),
                        "Bahia Concha",
                        RescueStatus
                                .IN_REHABILITATION,
                        "DB-CAR",
                        "AN-2026-001"
                );
        when(
                service.findByCode(
                        "RES-2026-001"
                )
        ).thenReturn(response);
        mockMvc.perform(
                        get(
                                "/api/rescue-cases/{code}",
                                "RES-2026-001"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.caseCode")
                                .value("RES-2026-001")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(
                                        "IN_REHABILITATION"
                                )
                );
        verify(service)
                .findByCode(
                        "RES-2026-001"
                );
    }
}