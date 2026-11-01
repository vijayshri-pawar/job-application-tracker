package com.vijayshri.jobtrackerbackend.controller;

import com.vijayshri.jobtrackerbackend.config.CorsConfig;
import com.vijayshri.jobtrackerbackend.config.JwtConfig;
import com.vijayshri.jobtrackerbackend.config.SecurityConfig;
import com.vijayshri.jobtrackerbackend.dto.JobApplicationResponse;
import com.vijayshri.jobtrackerbackend.exception.ResourceNotFoundException;
import com.vijayshri.jobtrackerbackend.model.Status;
import com.vijayshri.jobtrackerbackend.service.JobApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobApplicationController.class)
@Import({SecurityConfig.class, JwtConfig.class, CorsConfig.class})
class JobApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobApplicationService service;

    /** Pretends the request carries a valid JWT for user 1. */
    private static RequestPostProcessor user1() {
        return jwt().jwt(token -> token.subject("1"));
    }

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_withToken_usesUserIdFromToken() throws Exception {
        when(service.getAll(eq(1L), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/applications").with(user1()))
                .andExpect(status().isOk());
    }

    @Test
    void create_withValidBody_returns201() throws Exception {
        JobApplicationResponse saved = new JobApplicationResponse(1L, 2L, "Google", "Java Developer",
                LocalDate.of(2026, 10, 30), Status.APPLIED, null, "Pune", null, null);
        when(service.add(eq(1L), any())).thenReturn(saved);

        mockMvc.perform(post("/api/applications").with(user1())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId": 2, "role": "Java Developer", "dateApplied": "2026-10-30",
                                 "status": "Applied", "location": "Pune"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.company").value("Google"))
                .andExpect(jsonPath("$.status").value("Applied"));
    }

    @Test
    void create_withMissingFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/applications").with(user1())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.companyId").value("Company is required"))
                .andExpect(jsonPath("$.errors.role").value("Role title is required"));
    }

    @Test
    void create_withUnknownStatus_returns400() throws Exception {
        mockMvc.perform(post("/api/applications").with(user1())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId": 2, "role": "Dev", "dateApplied": "2026-10-30", "status": "Hired"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_whenMissing_returns404() throws Exception {
        when(service.getById(1L, 42L)).thenThrow(new ResourceNotFoundException("Job application", 42L));

        mockMvc.perform(get("/api/applications/42").with(user1()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job application not found with id 42"));
    }

    @Test
    void getById_withNonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/applications/abc").with(user1()))
                .andExpect(status().isBadRequest());
    }
}
