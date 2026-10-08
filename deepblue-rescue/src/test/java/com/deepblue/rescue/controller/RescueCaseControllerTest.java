package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.RescueCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

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
    @Test
    void shouldReturn404WhenCaseDoesNotExist()
            throws Exception {
        when(
                service.findByCode("RES-999")
        ).thenThrow(
                new ResourceNotFoundException(
                        "Rescue case not found: RES-999"
                )
        );
        mockMvc.perform(
                        get(
                                "/api/rescue-cases/{code}",
                                "RES-999"
                        )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.timestamp")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Rescue case not found: RES-999"
                                )
                )
                .andExpect(
                        jsonPath("$.details")
                                .isMap()
                );
    }
    @Test
    void shouldReturnCasesByStatus() throws Exception {

        RescueCaseResponse case1 = new RescueCaseResponse(
                1L, "RES-2026-001", LocalDate.of(2026, 8, 20),
                "Bahia Concha", RescueStatus.IN_REHABILITATION,
                "DB-CAR", "AN-2026-001");

        RescueCaseResponse case2 = new RescueCaseResponse(
                2L, "RES-2026-002", LocalDate.of(2026, 8, 22),
                "Taganga", RescueStatus.IN_REHABILITATION,
                "DB-CAR", "AN-2026-002");

        when(service.findByStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(case1, case2));

        mockMvc.perform(get("/api/rescue-cases")
                        .param("status", "IN_REHABILITATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("IN_REHABILITATION"))
                .andExpect(jsonPath("$[1].status").value("IN_REHABILITATION"));
    }
    @Test
    void shouldReturn400WhenStatusParamIsInvalid() throws Exception {

        mockMvc.perform(get("/api/rescue-cases")
                        .param("status", "FLYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.status").exists());
    }
}