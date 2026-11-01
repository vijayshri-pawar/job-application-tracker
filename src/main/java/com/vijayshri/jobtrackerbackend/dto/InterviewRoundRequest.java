package com.vijayshri.jobtrackerbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record InterviewRoundRequest(

        @NotNull(message = "Application id is required")
        Long applicationId,

        @NotBlank(message = "Round name is required")
        @Size(max = 100, message = "Round name must be at most 100 characters")
        String roundName,

        LocalDate roundDate,

        @Size(max = 50, message = "Result must be at most 50 characters")
        String result,

        @Size(max = 1000, message = "Failure reason must be at most 1000 characters")
        String failureReason,

        @Size(max = 1000, message = "Mistakes must be at most 1000 characters")
        String mistakes,

        @Size(max = 1000, message = "Improvements must be at most 1000 characters")
        String improvements
) {
}
