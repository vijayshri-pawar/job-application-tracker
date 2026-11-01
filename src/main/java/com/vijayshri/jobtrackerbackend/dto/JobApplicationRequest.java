package com.vijayshri.jobtrackerbackend.dto;

import com.vijayshri.jobtrackerbackend.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

/** Data the client sends to create or update a job application. */
public record JobApplicationRequest(

        @NotNull(message = "Company is required")
        Long companyId,

        @NotBlank(message = "Role title is required")
        @Size(max = 150, message = "Role title must be at most 150 characters")
        String role,

        @NotNull(message = "Date applied is required")
        @PastOrPresent(message = "Date applied cannot be in the future")
        LocalDate dateApplied,

        @NotNull(message = "Status is required")
        Status status,

        @URL(message = "Job link must be a valid URL")
        @Size(max = 500, message = "Job link must be at most 500 characters")
        String jobLink,

        @Size(max = 150, message = "Location must be at most 150 characters")
        String location
) {
}
